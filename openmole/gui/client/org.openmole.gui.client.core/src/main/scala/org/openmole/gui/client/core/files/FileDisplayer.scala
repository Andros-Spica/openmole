package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.client.core._
import org.openmole.gui.ext.data.DataUtils._

import scala.concurrent.Future

/*
 * Copyright (C) 07/05/15 // mathieu.leclaire@openmole.org
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

object FileDisplayer {
  def editor(fileType: FileExtension, initCode: String): EditorPanelUI = EditorPanelUI(fileType, initCode)

}

class FileDisplayer(val tabs: TreeNodeTabs) {

  def alreadyDisplayed(safePath: SafePath) =
    tabs.tabs.now.find { t ⇒
      t.safePathTab.now.path == safePath.path
    }

  def display(safePath: SafePath, content: String, fileExtension: FileExtension) = {
    alreadyDisplayed(safePath) match {
      case Some(t: TreeNodeTab) ⇒ tabs.setActive(t)
      case _ ⇒ fileExtension match {
        case OpenMOLEScript ⇒
          tabs ++ TreeNodeTab.oms(safePath, content)
        case MDScript ⇒ post()[Api].mdToHtml(safePath).call().foreach { htmlString ⇒
          tabs ++ TreeNodeTab.html(safePath, htmlString)
        }
        case SVGExtension ⇒ tabs ++ TreeNodeTab.html(safePath, content)
        case dod: EditableOnDemandFile ⇒
          if (DataUtils.isCSV(safePath)) {
            post()[Api].sequence(safePath).call().foreach { seq ⇒
              tabs ++ TreeNodeTab.editable(safePath, content, seq, TreeNodeTab.Table)
            }
          }
          else {
            tabs ++ TreeNodeTab.editable(safePath, content, Seq(), TreeNodeTab.Raw)
          }
        case _ ⇒ //FIXME for GUI workflows
      }
    }
  }

  def getSequence(safePath: SafePath): Future[Seq[Array[String]]] = {
    val fileExtension: FileExtension = safePath.name
    fileExtension match {
      case FileExtension.CSV ⇒ post()[Api].sequence(safePath).call()
      case _                 ⇒ Future(Seq())
    }
  }

}
