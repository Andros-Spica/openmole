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

package org.openmole.core.workflow.job

import java.util.UUID

import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.task._
import org.openmole.core.context._

object MoleJob {
  implicit val moleJobOrdering = Ordering.by((_: MoleJob).id)

  type StateChangedCallBack = (MoleJob, State, State) ⇒ Unit
  def apply(
    task:                 Task,
    context:              Context,
    id:                   UUID,
    stateChangedCallBack: MoleJob.StateChangedCallBack
  ) = {
    val (prototypes, values) = compressContext(context)
    new MoleJob(task, prototypes.toArray, values.toArray, id.getMostSignificantBits, id.getLeastSignificantBits, stateChangedCallBack)
  }
  def compressContext(context: Context) =
    context.toSeq.map {
      case (_, v) ⇒ (v.asInstanceOf[Variable[Any]].prototype, v.value)
    }.unzip

  sealed trait StateChange
  case object Unchanged extends StateChange
  case class Changed(old: State, state: State) extends StateChange
}

import MoleJob._

class MoleJob(
    val task:               Task,
    private var prototypes: Array[Val[Any]],
    private var values:     Array[Any],
    mostSignificantBits:    Long, leastSignificantBits: Long,
    stateChangedCallBack: MoleJob.StateChangedCallBack
) {

  var exception: Option[Throwable] = None

  @volatile private var _state: State = READY

  def state: State = _state
  def context: Context =
    Context((prototypes zip values).map { case (p, v) ⇒ Variable(p, v) }: _*)

  private def context_=(ctx: Context) = {
    val (_prototypes, _values) = MoleJob.compressContext(ctx)
    prototypes = _prototypes.toArray
    values = _values.toArray
  }

  def id = new UUID(mostSignificantBits, leastSignificantBits)

  private def changeState(state: State) = synchronized {
    if (!_state.isFinal) {
      val oldState = _state
      _state = state
      Changed(oldState, state)
    }
    else Unchanged
  }

  private def signalChanged(change: StateChange) =
    change match {
      case Changed(old, state) ⇒ stateChangedCallBack(this, old, state)
      case _                   ⇒
    }

  private def state_=(state: State) = signalChanged(changeState(state))

  def perform(executionContext: TaskExecutionContext) =
    if (!state.isFinal) {
      try {
        state = RUNNING
        context = task.perform(context, executionContext)
        state = COMPLETED
      }
      catch {
        case t: Throwable ⇒
          exception = Some(t)
          state = FAILED
      }
    }

  def finish(_context: Context) = {
    val changed =
      synchronized {
        if (!finished) context = _context
        changeState(COMPLETED)
      }

    signalChanged(changed)
  }

  def finished: Boolean = state.isFinal

  def cancel = state = CANCELED

}
