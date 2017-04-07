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

package org.openmole.core.workflow.execution

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.tools.service._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.workflow.execution.local.{ ExecutorPool, LocalExecutionJob }
import org.openmole.core.workflow.job.{ Job, MoleJob }
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools.{ ExceptionEvent, Name }
import org.openmole.tool.cache._
import org.openmole.tool.collection._

import scala.ref.WeakReference

object Environment {
  val maxExceptionsLog = ConfigurationLocation("Environment", "MaxExceptionsLog", Some(1000))

  case class JobSubmitted(job: ExecutionJob) extends Event[Environment]
  case class JobStateChanged(job: ExecutionJob, newState: ExecutionState, oldState: ExecutionState) extends Event[Environment]
  case class ExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level) extends Event[Environment] with ExceptionEvent
  case class MoleJobExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level, moleJob: MoleJob) extends Event[Environment] with ExceptionEvent
  case class JobCompleted(job: ExecutionJob, log: RuntimeLog, info: RuntimeInfo) extends Event[Environment]

  case class RuntimeLog(beginTime: Long, executionBeginTime: Long, executionEndTime: Long, endTime: Long)
}

import org.openmole.core.workflow.execution.Environment._

sealed trait Environment <: Name {
  private[execution] val _done = new AtomicLong(0L)
  private[execution] val _failed = new AtomicLong(0L)

  implicit def preference: Preference
  implicit def eventDispatcher: EventDispatcher

  private lazy val _errors = new SlidingList[ExceptionEvent]
  def error(e: ExceptionEvent) = _errors.put(e, preference(maxExceptionsLog))
  def errors: List[ExceptionEvent] = _errors.elements
  def clearErrors: List[ExceptionEvent] = _errors.clear()

  def submitted: Long
  def running: Long
  def done: Long = _done.get()
  def failed: Long = _failed.get()

}

trait SubmissionEnvironment <: Environment {
  def submit(job: Job)
  def jobs: Iterable[ExecutionJob]
}

object LocalEnvironment {

  def apply(
    nbThreads:    OptionalArgument[Int]    = None,
    deinterleave: Boolean                  = false,
    name:         OptionalArgument[String] = OptionalArgument()
  )(implicit varName: sourcecode.Name, preference: Preference, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher) = new LocalEnvironment(nbThreads.getOrElse(1), deinterleave, Some(name.getOrElse(varName.value)))

}

class LocalEnvironment(
    val nbThreads:     Int,
    val deinterleave:  Boolean,
    override val name: Option[String]
)(implicit val preference: Preference, threadProvider: ThreadProvider, val eventDispatcher: EventDispatcher) extends Environment {

  val pool = Cache(new ExecutorPool(nbThreads, WeakReference(this), threadProvider))

  def nbJobInQueue = pool().waiting

  def submit(job: Job, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, job.moleJobs, Some(job.moleExecution)))

  def submit(moleJob: MoleJob, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, List(moleJob), None))

  private def submit(ejob: LocalExecutionJob): Unit = {
    pool().enqueue(ejob)
    ejob.state = ExecutionState.SUBMITTED
    eventDispatcher.trigger(this, new Environment.JobSubmitted(ejob))
  }

  def submitted: Long = pool().waiting
  def running: Long = pool().running
}
