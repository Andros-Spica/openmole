/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.jvm

import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.plugin.task.external.ExternalTask
import java.io.File
import org.openmole.tool.file._

object JVMLanguageTask {
  lazy val workDirectory = Prototype[File]("workDirectory")
}

trait JVMLanguageTask extends ExternalTask with Plugins {

  def libraries: Seq[File]

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val pwd = executionContext.tmpDirectory.newDir("jvmtask")
    val preparedContext = prepareInputFiles(context, relativeResolver(pwd.getCanonicalFile)) + Variable(JVMLanguageTask.workDirectory, pwd)
    val resultContext = processCode(preparedContext)
    val resultContextWithFiles = fetchOutputFiles(resultContext, relativeResolver(pwd.getCanonicalFile))
    checkAndClean(resultContextWithFiles, pwd.getCanonicalFile)
    resultContextWithFiles
  }

  def processCode(context: Context)(implicit rng: RandomProvider): Context

}
