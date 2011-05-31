/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.hook

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.hook.CapsuleEvent
import org.openmole.core.model.hook.CapsuleEvent._
import org.openmole.core.model.hook.ICapsuleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution

abstract class CapsuleExecutionHook(moleExecution: IMoleExecution, capsule: IGenericCapsule, expectedEvent: CapsuleEvent.Value) extends ICapsuleExecutionHook {
  
  CapsuleExecutionDispatcher += (moleExecution, capsule, this)

  def process(moleJob: IMoleJob, event: CapsuleEvent.Value) = {
    if(expectedEvent == All || event == expectedEvent) process(moleJob)
  }
  
  def process(moleJob: IMoleJob)
}
