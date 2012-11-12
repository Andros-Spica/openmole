/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.domain.distribution

import scala.swing._
import swing.Swing._
import swing.ListView._
import scala.swing.Table.ElementMode._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import scala.swing.BorderPanel.Position._
import java.util.{ Locale, ResourceBundle }

class UniformDistributionPanelUI(pud: UniformDistributionDataUI[_],
                                 prototype: IPrototypeDataProxyUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))
  val maxField = new TextField(6)

  prototype.dataUI.toString match {
    case "Int" ⇒
      contents += (new Label("Size"), "gap para")
      contents += (maxField, "wrap")
      maxField.text = pud.max.getOrElse(0).toString
    case _ ⇒ contents += new Label("<html><i>No more information is required for this Domain</i></html>")
  }

  def saveContent = UniformDistributionDataUI({ if (maxField.text.isEmpty) scala.None else Some(maxField.text.toInt) },
    prototype.dataUI.toString)

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(maxField, new Help(i18n.getString("max"), i18n.getString("maxEx")))
  }
}
