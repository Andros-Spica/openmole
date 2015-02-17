package org.openmole.gui.client.core.dataui

import org.openmole.gui.client.core.GenericPanel
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui.{ PanelUI, DataUI }
import rx._

/*
 * Copyright (C) 28/01/15 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class InputDataUI(mappingsFactory: IOMappingsFactory) extends DataUI {

  type DATA = InputData

  def dataType = "Inputs"

  def +=(proto: PrototypeDataBagUI) =
    if (!exists(proto)) {
      //inputUI.mappings() = IOMappingsFactory(mappings: _*).build
      inputsUI() = inputsUI() :+ inputUI(proto, mappingsFactory.build)
    }

  def -=(inputUI: InputUI) = inputsUI() = inputsUI().filter {
    _.id != inputUI.id
  }

  def exists(inputUI: InputUI) = inputsUI().exists {
    _.id == inputUI.id
  }

  def exists(proto: PrototypeDataBagUI) = inputsUI().exists {
    _.protoDataBagUI.uuid == proto.uuid
  }

  val inputsUI: Var[Seq[InputUI]] = Var(Seq())

  def panelUI: PanelUI = PanelUI.empty

  def panelUI(panel: GenericPanel): PanelUI = new InputPanelUI(panel, this)

  val mappingKeys = mappingsFactory.build.fields.map { _.key }

  def data = new InputData {
    def inputs = inputsUI().map { id ⇒
      Input(id.protoDataBagUI.dataUI().data, id.mappings.fields.map { _.data })
    }
  }

}

