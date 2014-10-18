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

package org.openmole.ide.plugin.task.groovy

import java.awt.Dimension
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.misc.widget.GroovyEditor
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.panel.Settings
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import java.io.File

class GroovyTaskPanelUI(pud: GroovyTaskDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends TaskPanelUI {

  val codeTextArea = new GroovyEditor {
    editor.text = pud.code
    minimumSize = new Dimension(450, 200)
  }

  val libMultiTextField = new MultiChooseFileTextField("Libraries",
    pud.libs.map {
      l ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(l.getAbsolutePath))
    },
    "Select a file",
    FilesOnly,
    Some("Lib files", Seq("jar")),
    CLOSE_IF_EMPTY)

  val components = List(("Code", codeTextArea), ("Library", libMultiTextField.panel))

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(codeTextArea.editor,
    new Help(i18n.getString("groovyCode"),
      i18n.getString("groovyCodeEx"),
      List(new URL(i18n.getString("groovyURLText"), i18n.getString("groovyURL")))))

  add(libMultiTextField,
    new Help(i18n.getString("libraryPath"),
      i18n.getString("libraryPathEx")))

  def saveContent(name: String): TaskDataUI = new GroovyTaskDataUI010(name,
    codeTextArea.editor.text,
    libMultiTextField.content.map {
      _.content
    }.filterNot(_.isEmpty).map { new File(_) })
}
