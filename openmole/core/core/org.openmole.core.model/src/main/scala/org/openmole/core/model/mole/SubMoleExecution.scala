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

package org.openmole.core.model.mole

import org.openmole.core.model.data._
import org.openmole.core.model.transition._
import org.openmole.misc.eventdispatcher._
import org.openmole.core.model.job._
import org.openmole.core.model.task._
import org.openmole.core.model.tools._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.job.State._
import org.openmole.misc.exception._
import MoleJob._
import org.openmole.misc.tools.service.ThreadUtil.background
import scala.collection.mutable.Buffer

import scala.concurrent.stm._
import java.util.concurrent.{ Semaphore, locks, Executors }
import org.openmole.misc.tools.service.LockUtil._
import org.openmole.misc.tools.service.Logger

object SubMoleExecution extends Logger

import SubMoleExecution.Log._

class SubMoleExecution(
    val parent: Option[SubMoleExecution],
    val moleExecution: MoleExecution) extends ISubMoleExecution {

  @transient lazy val transitionLock = new locks.ReentrantLock()
  @transient lazy val masterCapsuleSemaphore = new Semaphore(1)

  private val _nbJobs = Ref(0)
  private val _childs = TSet.empty[SubMoleExecution]
  private val _jobs = TMap[IMoleJob, (ICapsule, ITicket)]()
  private val _canceled = Ref(false)

  val masterCapsuleRegistry = new RegistryWithTicket[IMasterCapsule, Context]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[Variable[_]]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Iterable[Variable[_]]]

  parentApply(_.+=(this))

  override def canceled: Boolean = atomic { implicit txn ⇒
    _canceled() || (parent match {
      case Some(p) ⇒ p.canceled
      case None    ⇒ false
    })
  }

  private def rmJob(moleJob: IMoleJob) = atomic { implicit txn ⇒
    _jobs.remove(moleJob)
    nbJobs_+=(-1)
  }

  private def nbJobs_+=(v: Int): Unit = atomic { implicit txn ⇒
    _nbJobs += v
    parentApply(_.nbJobs_+=(v))
  }

  def numberOfJobs = _nbJobs.single()

  override def root = !parent.isDefined

  override def cancel = {
    atomic { implicit txn ⇒
      _canceled() = true
      cancelJobs
      TSet.asSet(_childs)
    }.foreach { _.cancel }
    parentApply(_.-=(this))
  }

  def cancelJobs = _jobs.single.keys.foreach { _.cancel }

  override def childs = _childs.single

  private def +=(submoleExecution: SubMoleExecution) =
    _childs.single += submoleExecution

  private def -=(submoleExecution: SubMoleExecution) =
    _childs.single -= submoleExecution

  override def jobs =
    atomic {
      implicit txn ⇒
        (_jobs.keys ++ TSet.asSet(_childs).toSeq.flatMap(_.jobs)).toSeq
    }

  private def jobFailedOrCanceled(job: IMoleJob) = {
    val (capsule, ticket) = _jobs.single.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))

    val finished =
      atomic { implicit txn ⇒
        rmJob(job)
        isFinished
      }
    if (finished) finish(ticket)

    moleExecution.jobFailedOrCanceled(job, capsule)
  }

  private def jobFinished(job: IMoleJob) = {
    val mole = moleExecution.mole
    val (capsule, ticket) = _jobs.single(job)
    try {
      val ctxForHooks = moleExecution.implicits + job.context

      def executeHook(h: IHook) =
        try h.perform(ctxForHooks, moleExecution.executionContext)
        catch {
          case e: Throwable ⇒
            EventDispatcher.trigger(moleExecution, new IMoleExecution.HookExceptionRaised(h, job, e, SEVERE))
            logger.log(SEVERE, "Error in execution of misc " + h + "at the end of task " + job.task, e)
            throw e
        }

      val context = job.context ++ moleExecution.hooks(capsule).flatMap(executeHook).unzip._2
      mole.outputDataChannels(capsule).foreach { _.provides(context, ticket, moleExecution) }

      transitionLock {
        mole.outputTransitions(capsule).toList.sortBy(t ⇒ mole.slots(t.end.capsule).size).reverse.foreach { _.perform(context, ticket, this) }
      }
    }
    catch {
      case t: Throwable ⇒
        logger.log(SEVERE, "Error in submole execution", t)
        EventDispatcher.trigger(moleExecution, new IMoleExecution.ExceptionRaised(job, t, SEVERE))
        throw t
    }
    finally {
      val finished =
        atomic { implicit txn ⇒
          rmJob(job)
          isFinished
        }
      if (finished) finish(ticket)
      moleExecution.jobOutputTransitionsPerformed(job, capsule)
    }
  }

  private def isFinished = _nbJobs.single() == 0

  private def finish(ticket: ITicket) = {
    EventDispatcher.trigger(this, new ISubMoleExecution.Finished(ticket))
    parentApply(_.-=(this))
  }

  override def submit(capsule: ICapsule, context: Context, ticket: ITicket) = {
    if (!canceled) {
      nbJobs_+=(1)

      def addJob(moleJob: IMoleJob, capsule: ICapsule, ticket: ITicket) = atomic { implicit txn ⇒
        _jobs.put(moleJob, (capsule, ticket))
      }

      def implicits =
        moleExecution.implicits + Variable(Task.openMOLESeed, moleExecution.newSeed)

      val sourced =
        moleExecution.sources(capsule).foldLeft(Context.empty) {
          case (a, s) ⇒
            val ctx = try s.perform(implicits + context, moleExecution.executionContext)
            catch {
              case t: Throwable ⇒
                logger.log(SEVERE, "Error in submole execution", t)
                EventDispatcher.trigger(moleExecution, new IMoleExecution.SourceExceptionRaised(s, capsule, t, SEVERE))
                throw new InternalProcessingError(t, s"Error in source execution that is plugged to $capsule")
            }
            a + ctx
        }

      //FIXME: Factorize code
      capsule match {
        case c: IMasterCapsule ⇒
          def stateChanged(job: IMoleJob, oldState: State, newState: State) =
            EventDispatcher.trigger(moleExecution, new IMoleExecution.JobStatusChanged(job, c, newState, oldState))

          background {
            masterCapsuleSemaphore {
              val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(Context.empty)
              val moleJob: IMoleJob = MoleJob(capsule.task, implicits + sourced + context + savedContext, moleExecution.nextJobId, stateChanged)
              EventDispatcher.trigger(moleExecution, new IMoleExecution.JobCreated(moleJob, capsule))
              addJob(moleJob, capsule, ticket)
              moleJob.perform
              masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(moleJob.context))
              finalState(moleJob, moleJob.state)
            }
          }
        case _ ⇒
          def stateChanged(job: IMoleJob, oldState: State, newState: State) = {
            EventDispatcher.trigger(moleExecution, new IMoleExecution.JobStatusChanged(job, capsule, newState, oldState))
            if (newState.isFinal) finalState(job, newState)
          }

          val moleJob: IMoleJob = MoleJob(capsule.task, implicits + sourced + context, moleExecution.nextJobId, stateChanged)
          addJob(moleJob, capsule, ticket)
          EventDispatcher.trigger(moleExecution, new IMoleExecution.JobCreated(moleJob, capsule))
          moleExecution.group(moleJob, capsule, this)
      }

    }
  }

  def newChild: ISubMoleExecution = {
    val subMole = new SubMoleExecution(Some(this), moleExecution)
    if (canceled) subMole.cancel
    subMole
  }

  private def parentApply(f: SubMoleExecution ⇒ Unit) =
    parent match {
      case None    ⇒
      case Some(p) ⇒ f(p)
    }

  def finalState(job: IMoleJob, state: State) = {
    job.exception match {
      case Some(e) ⇒
        val (capsule, _) = _jobs.single(job)
        logger.log(SEVERE, s"Error in user job execution for capsule $capsule, job state is FAILED.", e)
        EventDispatcher.trigger(moleExecution, IMoleExecution.JobFailed(job, capsule, e))
      case _ ⇒
    }

    if (state == COMPLETED) {
      val (capsule, _) = _jobs.single(job)
      EventDispatcher.trigger(moleExecution, IMoleExecution.JobFinished(job, capsule))
    }

    state match {
      case COMPLETED         ⇒ jobFinished(job)
      case FAILED | CANCELED ⇒ jobFailedOrCanceled(job)
      case _                 ⇒
    }
  }

}
