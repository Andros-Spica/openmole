/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.ssh

import fr.iscpif.gridscale.ssh.SSHConnectionCache
import org.openmole.core.communication.storage.RemoteStorage
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.batch.storage.StorageService
import org.openmole.plugin.environment.gridscale.GridScaleStorage
import squants.time.TimeConversions._

trait SSHStorageService extends StorageService with SSHService with GridScaleStorage { ss ⇒

  val environment: BatchEnvironment with SSHAccess

  lazy val storage =
    new fr.iscpif.gridscale.ssh.SSHStorage with SSHConnectionCache {
      override def timeout = environment.services.preference(SSHService.timeout)
      override def credential = environment.credential
      override def host: String = environment.host
      override def port: Int = environment.port
    }

  lazy val home = storage.home

  lazy val remoteStorage: RemoteStorage = new RemoteLogicalLinkStorage(ss.root)

}
