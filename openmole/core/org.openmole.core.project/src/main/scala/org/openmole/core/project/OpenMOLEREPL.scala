/**
 * Created by Romain Reuillon on 22/01/16.
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.core.project

import org.openmole.core.console.ScalaREPL
import org.openmole.core.dsl._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager.PluginInfo
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.NewFile

object OpenMOLEREPL {

  def autoImports: Seq[String] = PluginInfo.pluginsInfo.toSeq.flatMap(_.namespaces).map(n ⇒ s"$n._")
  def keywordNamespace = "om"

  def keywordNamespaceCode = {
    def withPart = {
      val keyWordTraits = PluginInfo.pluginsInfo.flatMap(_.keywordTraits)
      if (keyWordTraits.isEmpty) ""
      else s"""with ${PluginInfo.pluginsInfo.flatMap(_.keywordTraits).mkString(" with ")}"""
    }
    s"""
       |object $keywordNamespace extends ${classOf[DSLPackage].getCanonicalName} $withPart
     """.stripMargin
  }

  def dslImport = Seq(
    classOf[org.openmole.core.dsl.DSLPackage].getPackage.getName + "._"
  ) ++
    autoImports

  def imports = initialisationCommands(dslImport).mkString("\n")

  def initialisationCommands(imports: Seq[String]) =
    Seq(
      imports.map("import " + _).mkString("; "),
      keywordNamespaceCode
    )

  def newREPL(args: ConsoleVariables, quiet: Boolean = false)(implicit newFile: NewFile, fileService: FileService) = {
    def initialise(loop: ScalaREPL) = {
      args.workDirectory.mkdirs()
      loop.beQuietDuring {
        (loop interpret imports) match {
          case scala.tools.nsc.interpreter.Results.Error ⇒
            throw new InternalProcessingError(s"Error while interpreting imports: ${imports}")
          case _ ⇒
        }
        ConsoleVariables.bindVariables(loop, args)
      }
      loop
    }

    initialise(ScalaREPL(quiet = quiet))
  }
}
