/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.environment.desktopgrid

import java.text.NumberFormat
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.FormattedTextField
import scala.swing.Label
import scala.swing.TextField
import scala.swing.PasswordField

class DesktopGridEnvironmentPanelUI(pud: DesktopGridEnvironmentDataUI) extends MigPanel("fillx,wrap 2","[left][grow,fill]","") with IEnvironmentPanelUI{
  val loginTextField = new TextField(20)
  val passTextField = new TextField(20)
  val portTextField = new TextField(5)
  
  contents+= (new Label("Login"),"gap para")
  contents+= loginTextField
  contents+= (new Label("Password"),"gap para")
  contents+= passTextField
  contents+= (new Label("Port"),"gap para")
  contents+= portTextField
  
  loginTextField.text = pud.login
  passTextField.text = pud.pass
  portTextField.text = pud.port.toString
  
  override def saveContent(name: String) = new DesktopGridEnvironmentDataUI(name,
                                                                            loginTextField.text,
                                                                            passTextField.text,
                                                                            portTextField.text.toInt)
}
