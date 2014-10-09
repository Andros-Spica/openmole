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

import java.awt.Color
import java.awt.Dimension
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.builder.Builder
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.workflow.{ ISceneContainer, BuildMoleSceneContainer }
import scala.collection.JavaConversions._
import org.openmole.ide.core.implementation.factory.FactoryUI
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.data._
import scala.swing.event.ButtonClicked
import scala.Some

object ConceptMenu {

  def createAndDisplaySamplingComposition = display(Builder.samplingCompositionUI(false))
  def createAndDisplayPrototype = display(Builder.prototypeUI)
  def createAndDisplayHook = display(Builder.hookUI(false))
  def createAndDisplaySource = display(Builder.sourceUI(false))
  def createAndDisplayEnvironment = display(Builder.environmentUI(false))

  val menuItemMapping = new HashMap[DataProxyUI, MenuItem]()
  val mapping = new HashMap[List[String], Menu]

  def menu(seq: List[String]): Menu = {
    def menu0(seq: List[String], m: Menu): Menu = {
      if (seq.isEmpty) m
      else menu0(seq.tail, mapping.getOrElseUpdate(seq, {
        val child = new Menu(seq.head)
        m.contents += child
        m
      }))
    }
    menu0(seq.tail, mapping.getOrElseUpdate(List(seq.head), new Menu(seq.head)))
  }

  def menuItem[T](proxy: T, fact: FactoryUI, f: T ⇒ Unit, popup: PopupToolBarPresenter): MenuItem = {
    if (fact.category.isEmpty) menuItem(proxy, fact.toString, f)
    else {
      val m = menu(fact.category)
      val item = menuItem(proxy, fact.toString, f)
      popup.listenTo(item)
      m.contents += item
      m
    }
  }

  def menuItem[T](proxy: T, s: String, f: T ⇒ Unit): MenuItem =
    new MenuItem(new Action(s) {
      override def apply = f(proxy)
    }) {
      listenTo(this)
      reactions += {
        case x: ButtonClicked ⇒
          publish(new ConceptChanged(this))
      }
    }

  def menuItem(f: ⇒ Unit): MenuItem = new MenuItem(new Action("New") {
    override def apply = {
      ScenesManager().closePropertyPanels
      f
    }
  })

  val taskMenu = new PopupToolBarPresenter("Task", new Color(107, 138, 166), List(menuItem(display(Builder.taskUI(false)))))
  val environmentMenu = new PopupToolBarPresenter("Environment", new Color(68, 120, 33), List(menuItem(display(Builder.environmentUI(false)))))
  val prototypeMenu = new PopupToolBarPresenter("Prototype", new Color(192, 154, 0), List(menuItem(display(Builder.prototypeUI))))
  val samplingMenu = new PopupToolBarPresenter("Sampling", new Color(255, 85, 85), List(menuItem(display(Builder.samplingCompositionUI(false)))))
  val sourceMenu = new PopupToolBarPresenter("Source", new Color(99, 86, 136), List(menuItem(display(Builder.sourceUI(false)))))
  val hookMenu = new PopupToolBarPresenter("Hook", new Color(168, 120, 33), List(menuItem(display(Builder.hookUI(false)))))

  def factoryName(d: DataUI, factories: List[DataProxyFactory]): String = {
    List(factories.find { f ⇒ f.buildDataProxyUI.dataUI.getClass.isAssignableFrom(d.getClass) }).flatten.map {
      _.factory.toString
    }.headOption.getOrElse("")
  }

  def buildTaskMenu(f: TaskDataProxyUI ⇒ Unit, initDataUI: TaskDataUI) = {
    mapping.clear
    val factories = KeyRegistry.tasks.map {
      f ⇒ new TaskDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)
    // Enforce to build PopupToolBarPresenter, cause Memu.contents still return an empty buffer (Bug SI-2362),
    // so that it can be listened in the contructor as it is done the simple MenuItem
    val popup = new PopupToolBarPresenter(factoryName(initDataUI, factories), new Color(107, 138, 166))
    val items = factories.map { d ⇒ menuItem(d.buildDataProxyUI, d.factory, f, popup) }
    items.foreach { popup+= }
    popup
  }

  def buildEnvironmentMenu(f: EnvironmentDataProxyUI ⇒ Unit, initDataUI: EnvironmentDataUI) = {
    mapping.clear
    val factories = KeyRegistry.environments.map {
      f ⇒ new EnvironmentDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)

    val popup = new PopupToolBarPresenter(factoryName(initDataUI, factories), new Color(68, 120, 33))
    val items = factories.map { d ⇒ menuItem(d.buildDataProxyUI, d.factory, f, popup) }
    items.foreach { popup+= }
    popup
  }

  def buildPrototypeMenu(f: PrototypeDataProxyUI ⇒ Unit, initDataUI: PrototypeDataUI[_]) = {
    mapping.clear

    val pmenu = (GenericPrototypeDataUI.base ::: GenericPrototypeDataUI.extra).sortBy(_.toString).map {
      d ⇒ menuItem(PrototypeDataProxyUI(d), d.toString, f)
    }
    new PopupToolBarPresenter(initDataUI.typeClassString.split('.').last, new Color(192, 154, 0), pmenu)
  }

  def buildHookMenu(f: HookDataProxyUI ⇒ Unit, initDataUI: HookDataUI) = {
    mapping.clear
    val factories = KeyRegistry.hooks.map {
      f ⇒ new HookDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)

    val popup = new PopupToolBarPresenter(factoryName(initDataUI, factories), new Color(168, 120, 33))
    val items = factories.map { d ⇒ menuItem(d.buildDataProxyUI, d.factory, f, popup) }
    items.foreach { popup+= }
    popup
  }

  def buildSourceMenu(f: SourceDataProxyUI ⇒ Unit, initDataUI: SourceDataUI) = {
    mapping.clear
    val factories = KeyRegistry.sources.map {
      f ⇒ new SourceDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)

    val popup = new PopupToolBarPresenter(factoryName(initDataUI, factories), new Color(99, 86, 136))
    val items = factories.map { d ⇒ menuItem(d.buildDataProxyUI, d.factory, f, popup) }
    items.foreach { popup+= }
    popup
  }

  def -=(proxy: DataProxyUI) = {
    proxy match {
      case x: EnvironmentDataProxyUI         ⇒ environmentMenu.remove(menuItemMapping(proxy))
      case x: PrototypeDataProxyUI           ⇒ prototypeMenu.remove(menuItemMapping(proxy))
      case x: TaskDataProxyUI                ⇒ taskMenu.remove(menuItemMapping(proxy))
      case x: SamplingCompositionDataProxyUI ⇒ samplingMenu.remove(menuItemMapping(proxy))
      case x: HookDataProxyUI                ⇒ hookMenu.remove(menuItemMapping(proxy))
      case x: SourceDataProxyUI              ⇒ sourceMenu.remove(menuItemMapping(proxy))
    }
  }

  def +=(name: String, proxy: DataProxyUI) = addInMenu(proxy, addItem(name, proxy))

  def +=(proxy: DataProxyUI) = addInMenu(proxy, addItem(proxy))

  private def addInMenu(proxy: DataProxyUI, menuItem: MenuItem) = proxy match {
    case x: EnvironmentDataProxyUI         ⇒ environmentMenu.popup.contents += menuItem
    case x: PrototypeDataProxyUI           ⇒ prototypeMenu.popup.contents += menuItem
    case x: TaskDataProxyUI                ⇒ taskMenu.popup.contents += menuItem
    case x: SamplingCompositionDataProxyUI ⇒ samplingMenu.popup.contents += menuItem
    case x: HookDataProxyUI                ⇒ hookMenu.popup.contents += menuItem
    case x: SourceDataProxyUI              ⇒ sourceMenu.popup.contents += menuItem
  }

  def menuBar = new MenuBar {
    contents.append(prototypeMenu, taskMenu, samplingMenu, environmentMenu, hookMenu)
    minimumSize = new Dimension(size.width, 50)
  }

  def display(proxy: DataProxyUI) = {
    if (ScenesManager().tabPane.peer.getTabCount == 0) createTab(proxy)
    else ScenesManager().tabPane.selection.page.content match {
      case x: ISceneContainer ⇒ x.scene.displayPropertyPanel(proxy)
      case _                  ⇒ createTab(proxy)
    }
  }

  def createTab(proxy: DataProxyUI) = DialogFactory.newTabName match {
    case Some(y: BuildMoleSceneContainer) ⇒ y.scene.displayPropertyPanel(proxy)
    case _                                ⇒
  }

  def addItem(proxy: DataProxyUI): MenuItem = addItem(proxyName(proxy), proxy)

  def addItem(name: String,
              proxy: DataProxyUI): MenuItem = {
    menuItemMapping += proxy -> new MenuItem(new Action(proxyName(proxy)) {
      override def apply = {
        ConceptMenu.display(proxy)
      }
    })
    menuItemMapping(proxy)
  }

  def refreshItem(proxy: DataProxyUI) = {
    if (menuItemMapping.contains(proxy))
      menuItemMapping(proxy).action.title = proxyName(proxy)
  }

  def clearAllItems = {
    List(samplingMenu, prototypeMenu, taskMenu, environmentMenu, hookMenu, sourceMenu).foreach {
      _.removeAll
    }
    menuItemMapping.clear
  }

  def proxyName(proxy: DataProxyUI) =
    proxy.dataUI.name + (proxy match {
      case x: PrototypeDataProxyUI ⇒
        if (x.dataUI.dim > 0) " [" + x.dataUI.dim.toString + "]" else ""
      case _ ⇒ ""
    })

}
