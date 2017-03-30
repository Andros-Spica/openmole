/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment.batch.refresh

import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.logger.Logger

object KillerActor extends Logger {

  def receive(msg: KillBatchJob)(implicit services: BatchEnvironment.Services) = {
    import services._

    val KillBatchJob(bj) = msg
    try bj.jobService.tryWithToken {
      case Some(t) ⇒ bj.kill(t)
      case None ⇒
        JobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
    } catch {
      case e: Throwable ⇒ Log.logger.log(Log.FINE, "Could not kill job.", e)
    }
  }

}
