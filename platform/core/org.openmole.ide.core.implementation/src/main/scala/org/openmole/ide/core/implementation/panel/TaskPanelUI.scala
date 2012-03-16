/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.panel

import java.awt.BorderLayout
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.data.AbstractExplorationTaskDataUI
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.ImageLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import scala.swing.Action
import scala.swing.Panel
import scala.swing.Separator

class TaskPanelUI(proxy: ITaskDataProxyUI,
                  scene: IMoleScene,
                  mode: Value = CREATION) extends BasePanelUI(proxy, scene,mode,proxy.dataUI.borderColor){
  iconLabel.icon = new ImageIcon(ImageUtilities.loadImage(proxy.dataUI.fatImagePath))
  val panelUI = proxy.dataUI.buildPanelUI
  val protoPanel = new IOPrototypePanel
  mode match {
    case IO => protos
    case _=> properties
  }
  
  def create = {
    Proxys.tasks += proxy
    ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }
  
  def delete = {
    Proxys.tasks -= proxy
    ConceptMenu.removeItem(proxy)
  }
  
  def save = {
        proxy.dataUI = panelUI.save(nameTextField.text,protoPanel.protoIn.content,protoPanel.protoOut.content)
        proxy.dataUI match {
          case x : AbstractExplorationTaskDataUI => TopComponentsManager.capsules.filter(_.dataUI.task == proxy) match {
            case y : List[Nothing]=> 
            case y : List[ICapsuleUI] => y.head.addSampling(x.sampling)
          }
          case _ =>
        }
  }
  
  def switch = {
    save
    if(mainPanel.contents.size == 2) mainPanel.contents.remove(1)
    if(mainLinksPanel.contents.size == 2) mainLinksPanel.contents.remove(1)
    TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.closeExtraPropertyPanel
  }
  
  def properties = {
    switch    
    mainPanel.contents += panelUI.peer
    mainLinksPanel.contents +=  new ImageLinkLabel("img/next.png",new Action("") { def apply = protos })
    revalidate
    repaint
  }
  
  def protos : Unit = {
    switch
    mainPanel.contents += protoPanel.peer
    
    mainLinksPanel.contents +=  new ImageLinkLabel("img/previous.png",new Action("") { def apply = properties })
  }
  
  class IOPrototypePanel extends Panel{
    peer.setLayout(new BorderLayout)
    val image = new ImageIcon(ImageUtilities.loadImage("img/eye.png"))
      
    val emptyProto = EmptyDataUIs.emptyPrototypeProxy
    val protoIn = new MultiComboLinkLabelGroovyTextFieldEditor("Inputs",
                                                               TaskPanelUI.this.proxy.dataUI.prototypesIn.map{case(proto,v) => (proto,contentAction(proto),v)}.toList,
                                                               (List(emptyProto):::Proxys.prototypes.toList).map{p=>(p,contentAction(p))}.toList,
                                                               image)        
                                                                      
    val protoOut = new MultiComboLinkLabel("Outputs",
                                           TaskPanelUI.this.proxy.dataUI.prototypesOut.map{proto => (proto,contentAction(proto))}.toList,
                                           (List(emptyProto):::Proxys.prototypes.toList).map{p=>(p,contentAction(p))}.toList,
                                           image)        
    
    if (TaskPanelUI.this.proxy.dataUI.prototypesIn.isEmpty) protoIn.removeAllRows
    if (TaskPanelUI.this.proxy.dataUI.prototypesOut.isEmpty) protoOut.removeAllRows
    
    peer.add(protoIn.panel.peer,BorderLayout.WEST)
    peer.add((new Separator).peer)
    peer.add(protoOut.panel.peer,BorderLayout.EAST)
  
    def contentAction(proto : IPrototypeDataProxyUI) = new ContentAction(proto.dataUI.displayName,proto){
      override def apply = TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.displayExtraPropertyPanel(proto)} 
  }
}
