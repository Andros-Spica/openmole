/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.ssh

import org.openmole.plugin.environment.ssh.SSHEnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.workspace.Workspace

class SSHEnvironmentDataUI(val name: String = "",
                           val login: String = "",
                           val host: String = "",
                           val nbSlots: Int = 1,
                           val port: Int = 22,
                           val dir: String = "/tmp/",
                           val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                           var threads: Option[Int] = None) extends EnvironmentDataUI {

  def coreObject = util.Try {
    SSHEnvironment(login,
      host,
      nbSlots,
      port,
      Some(dir),
      openMOLEMemory,
      threads)(Workspace.authenticationProvider)
  }

  def coreClass = classOf[SSHEnvironment]

  override def imagePath = "img/ssh.png"

  override def fatImagePath = "img/ssh_fat.png"

  def buildPanelUI = new SSHEnvironmentPanelUI(this)
}
