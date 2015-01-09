/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.execution

import org.openmole.core.model.execution.ExecutionState._
import org.openmole.misc.eventdispatcher.EventDispatcher

trait ExecutionJob extends IExecutionJob {

  private var _state: ExecutionState = READY

  override def state = _state

  def state_=(state: ExecutionState) = synchronized {
    if (!this.state.isFinal) {
      EventDispatcher.trigger(environment, new Environment.JobStateChanged(this, state, this.state))
      _state = state
    }
  }

}
