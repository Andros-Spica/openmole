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
package org.openmole.ide.misc.widget

import scala.swing.FileChooser.SelectionMode._
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve
import scala.swing.TextField
import swing.event.{ KeyReleased, MousePressed }
import scala.collection.JavaConversions._
import scala.swing.event.Key.Enter

class ChooseFileTextField(initialText: String,
                          chooserTitle: String,
                          selectionMode: Value,
                          chooser: Option[(String, Seq[String])] = None,
                          toDoFunction: ⇒ Unit = {}) extends TextField {

  //def this(iT: String, cT: String, cD: String, ex: String, tdF: ⇒ Unit = {}) = this(iT, cT, Some(cD), FilesOnly, Some(ex), tdF)

  //def this(iT: String, cT: String) = this(iT, cT, None, FilesOnly, None)

  //def this(iT: String, f: ⇒ Unit = {}) = this(iT, "Select a directory", None, DirectoriesOnly, None, f)

  text = initialText
  val fc = new FileChooser {
    chooser.foreach { case (d, ext) ⇒ fileFilter = new FileNameExtensionFilter(d, ext: _*) }
    fileSelectionMode = selectionMode
    title = chooserTitle
  }

  reactions += {
    case e: MousePressed ⇒
      if (e.clicks == 2) {
        if (fc.showDialog(this, "OK") == Approve) text = fc.selectedFile.getPath
        publish(new DialogClosedEvent(this))
      }
    case KeyReleased(_, _, _, _) ⇒ toDoFunction
    case DialogClosedEvent(_)    ⇒ toDoFunction
  }

  columns = 15
  listenTo(this, this.mouse.clicks, keys)
}
