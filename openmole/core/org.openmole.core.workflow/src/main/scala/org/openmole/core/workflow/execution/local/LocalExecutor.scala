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

package org.openmole.core.workflow.execution.local

import java.io.{ OutputStream, PrintStream }

import org.openmole.core.event.EventDispatcher
import org.openmole.core.output.OutputManager
import org.openmole.core.tools.service
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution.Environment._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import org.openmole.tool.logger.Logger
import org.openmole.tool.stream._

import ref.WeakReference
import org.openmole.core.workflow.mole.{ MoleExecution, StrainerCapsule, StrainerTaskDecorator }
import org.openmole.core.event._
import org.openmole.tool.network.LocalHostName

object LocalExecutor extends Logger {

  def containsMoleTask(moleJob: MoleJob) =
    moleJob.task match {
      case _: MoleTask              ⇒ true
      case t: StrainerTaskDecorator ⇒ classOf[MoleTask].isAssignableFrom(t.task.getClass)
      case _                        ⇒ false
    }

}

class LocalExecutor(environment: WeakReference[LocalEnvironment]) extends Runnable {

  import LocalExecutor.Log._

  var stop: Boolean = false

  override def run = try {
    while (!stop) {
      environment.get match {
        case Some(environment) ⇒
          def jobGoneIdle() = {
            environment.pool().removeExecuter(this)
            environment.pool().addExecuter()
            stop = true
          }

          val executionJob = environment.pool().takeNextjob
          val beginTime = System.currentTimeMillis

          try {
            val (log, output) =
              withRedirectedOutput(executionJob, environment.deinterleave) {
                executionJob.state = ExecutionState.RUNNING

                for (moleJob ← executionJob.moleJobs) {
                  if (moleJob.state != State.CANCELED) {
                    if (LocalExecutor.containsMoleTask(moleJob)) jobGoneIdle()

                    moleJob.perform(executionJob.executionContext)
                    moleJob.exception match {
                      case Some(e) ⇒ environment.eventDispatcher.trigger(environment: Environment, MoleJobExceptionRaised(executionJob, e, SEVERE, moleJob))
                      case _       ⇒
                    }

                  }
                }

                executionJob.state = ExecutionState.DONE

                val endTime = System.currentTimeMillis
                RuntimeLog(beginTime, beginTime, endTime, endTime)
              }

            output.foreach {
              case Output(stream, output, error) ⇒
                display(stream, s"Output of local execution", output)
                display(stream, s"Error of local execution", error)
            }

            environment.eventDispatcher.trigger(environment: Environment, Environment.JobCompleted(executionJob, log, service.localRuntimeInfo))
          }
          catch {
            case e: InterruptedException ⇒ throw e
            case e: ThreadDeath          ⇒ throw e
            case e: Throwable ⇒
              val er = ExceptionRaised(executionJob, e, SEVERE)
              environment.error(er)
              logger.log(SEVERE, "Error in execution", e)
              environment.eventDispatcher.trigger(environment: Environment, er)
          }
          finally executionJob.state = ExecutionState.KILLED
        case None ⇒ stop = true
      }
    }
  }
  catch {
    case e: InterruptedException ⇒
    case e: ThreadDeath          ⇒
  }

  case class Output(stream: PrintStream, output: String, error: String)

  private def withRedirectedOutput[T](executionJob: LocalExecutionJob, deinterleave: Boolean)(f: ⇒ T) =
    executionJob.moleExecution match {
      case Some(execution) if deinterleave ⇒
        val (res, out) = OutputManager.withStringOutput(f)
        res → Some(Output(execution.executionContext.out, out.output, out.error))
      case Some(execution) ⇒
        val res = OutputManager.withStreamOutputs(execution.executionContext.out, execution.executionContext.out)(f)
        res → None
      case _ ⇒
        f → None
    }

}
