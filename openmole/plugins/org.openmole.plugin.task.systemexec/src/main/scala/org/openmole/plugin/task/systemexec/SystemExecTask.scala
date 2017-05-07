/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external._
import org.openmole.tool.random._

import cats.syntax.traverse._

object SystemExecTask {

  implicit def isTask: InputOutputBuilder[SystemExecTask] = InputOutputBuilder(SystemExecTask._config)
  implicit def isExternal: ExternalBuilder[SystemExecTask] = ExternalBuilder(SystemExecTask.external)
  implicit def isSystemExec = new SystemExecTaskBuilder[SystemExecTask] {
    override def commands = SystemExecTask.command
    override def environmentVariables = SystemExecTask.environmentVariables
    override def returnValue = SystemExecTask.returnValue
    override def errorOnReturnValue = SystemExecTask.errorOnReturnValue
    override def stdOut = SystemExecTask.stdOut
    override def stdErr = SystemExecTask.stdErr
    override def workDirectory = SystemExecTask.workDirectory
  }

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(commands: Command*)(implicit name: sourcecode.Name): SystemExecTask =
    new SystemExecTask(
      command = Vector.empty,
      workDirectory = None,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty,
      _config = InputOutputConfig(),
      external = External()
    ) set (pack.commands += (OS(), commands: _*))
}

@Lenses case class SystemExecTask(
    command:              Vector[OSCommands],
    workDirectory:        Option[String],
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Val[Int]],
    stdOut:               Option[Val[String]],
    stdErr:               Option[Val[String]],
    environmentVariables: Vector[(String, FromContext[String])],
    _config:              InputOutputConfig,
    external:             External
) extends Task with ValidateTask {

  import SystemExecTask._

  def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  override def validate =
    Validate { p ⇒
      import p._

      val allInputs = External.PWD :: inputs.toList

      val commandsError =
        for {
          c ← command
          exp ← c.expanded
          e ← exp.validate(allInputs)
        } yield e

      val variableErrors = environmentVariables.map(_._2).flatMap(_.validate(allInputs))

      commandsError ++ variableErrors ++ External.validate(external)(allInputs).apply
    }

  override protected def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    External.withWorkDir(executionContext) { tmpDir ⇒
      val workDir =
        workDirectory match {
          case None    ⇒ tmpDir
          case Some(d) ⇒ new File(tmpDir, d)
        }

      workDir.mkdirs()

      val context = p.context + (External.PWD → workDir.getAbsolutePath)

      val preparedContext = external.prepareInputFiles(context, external.relativeResolver(workDir))

      val osCommandLines =
        command.find { _.os.compatible }.map {
          cmd ⇒ cmd.expanded
        }.getOrElse(throw new UserBadDataError("No command line found for " + OS.actualOS))

      val executionResult = executeAll(
        workDir,
        environmentVariables.map { case (name, variable) ⇒ name → variable.from(context) },
        errorOnReturnValue,
        returnValue,
        stdOut,
        stdErr,
        osCommandLines.toList
      )(p.copy(context = preparedContext))

      val retContext: Context = external.fetchOutputFiles(outputs, preparedContext, external.relativeResolver(workDir), tmpDir)
      external.cleanWorkDirectory(outputs, retContext, tmpDir)

      retContext ++
        List(
          stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
          stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
          returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
        ).flatten
    }
  }

}
