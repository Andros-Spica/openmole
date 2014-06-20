/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.plugin.environment.glite.{ GliteEnvironment, DIRACGliteEnvironment }
import org.openmole.misc.workspace.Workspace
import util.Try
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.tools.service._

class DiracEnvironmentDataUI(val name: String = "",
                             val voName: String = "",
                             val service: String = "",
                             val group: String = "",
                             val bdii: String = Workspace.preference(GliteEnvironment.DefaultBDII),
                             val vomsURL: String = "",
                             val setup: String = "",
                             val fqan: Option[String] = None,
                             val cpuTime: Option[String] = None,
                             val openMOLEMemory: Option[Int] = None) extends EnvironmentDataUI {

  def coreObject = Try(DIRACGliteEnvironment(
    voName,
    service,
    Some(group),
    Some(bdii),
    Some(vomsURL),
    Some(setup),
    fqan,
    cpuTime.map(_.toDuration),
    openMOLEMemory)(Workspace.authenticationProvider))

  def coreClass = classOf[DIRACGliteEnvironment]

  override def imagePath = "img/dirac.png"

  override def fatImagePath = "img/dirac_fat.png"

  def buildPanelUI = new DiracEnvironmentPanelUI(this)
}
