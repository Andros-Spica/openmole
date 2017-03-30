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

package org.openmole.plugin.environment.pbs

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._

object PBSEnvironment {

  def apply(
    user:                 String,
    host:                 String,
    port:                 Int                           = 22,
    queue:                OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    wallTime:             OptionalArgument[Time]        = None,
    memory:               OptionalArgument[Information] = None,
    nodes:                OptionalArgument[Int]         = None,
    coreByNode:           OptionalArgument[Int]         = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._
    new PBSEnvironment(
      user = user,
      host = host,
      port = port,
      queue = queue,
      openMOLEMemory = openMOLEMemory,
      wallTime = wallTime,
      memory = memory,
      nodes = nodes,
      coreByNode = coreByNode,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = Some(name.getOrElse(varName.value))
    )(SSHAuthentication.find(user, host, port).apply)
  }
}

class PBSEnvironment(
    val user:                    String,
    val host:                    String,
    override val port:           Int,
    val queue:                   Option[String],
    override val openMOLEMemory: Option[Information],
    val wallTime:                Option[Time],
    val memory:                  Option[Information],
    val nodes:                   Option[Int],
    val coreByNode:              Option[Int],
    val sharedDirectory:         Option[String],
    val workDirectory:           Option[String],
    override val threads:        Option[Int],
    val storageSharedLocally:    Boolean,
    override val name:           Option[String]
)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication)(implicit val services: BatchEnvironment.Services) extends ClusterEnvironment { env ⇒

  type JS = PBSJobService

  lazy val jobService =
    new PBSJobService {
      def queue = env.queue
      val environment = env
      def sharedFS = storage
      def workDirectory = env.workDirectory
      def timeout = env.timeout
      def credential = env.credential
      def user = env.user
      def host = env.host
      def port = env.port
    }

}
