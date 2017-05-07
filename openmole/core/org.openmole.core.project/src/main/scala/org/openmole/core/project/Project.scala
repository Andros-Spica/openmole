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

import javax.script.CompiledScript

import org.openmole.core.console._
import org.openmole.core.pluginmanager._
import org.openmole.core.project.Imports.{ SourceFile, Tree }
import org.openmole.core.workflow.puzzle._
import org.openmole.tool.file._
import monocle.function.all._
import monocle.std.all._
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.fileservice.FileService
import org.openmole.core.services._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.hash._

object Project {

  def scriptExtension = ".oms"
  def isScript(file: File) = file.exists() && file.getName.endsWith(scriptExtension)
  def newREPL(variables: ConsoleVariables)(implicit newFile: NewFile, fileService: FileService) = OpenMOLEREPL.newREPL(variables, quiet = true)

  def uniqueName(source: File) = s"_${source.getCanonicalPath.hash()}"

  def scriptsObjects(script: File) = {

    def makeImportTree(tree: Tree): String =
      tree.children.map(c ⇒ makePackage(c.name, c.tree)).mkString("\n")

    def makePackage(name: String, tree: Tree): String =
      if (!tree.files.isEmpty) tree.files.distinct.map(f ⇒ makeVal(name, f)).mkString("\n")
      else
        s"""lazy val $name = new {
            |${makeImportTree(tree)}
            |}""".stripMargin

    def makeVal(identifier: String, file: File) =
      s"""lazy val ${identifier} = ${uniqueName(file)}"""

    def makeScript(sourceFile: SourceFile) = {
      def imports = makeImportTree(Tree.insertAll(sourceFile.importedFiles))

      val name = uniqueName(sourceFile.file)

      s"""class ${name}Class {
           |lazy val _imports = new {
           |$imports
           |}
           |
           |import _imports._
           |
           |private lazy val ${ConsoleVariables.workDirectory} = File(new java.net.URI("${sourceFile.file.getParentFileSafe.toURI}").getPath)
           |${sourceFile.file.content}
           |}
           |
           |lazy val ${name} = new ${name}Class
         """.stripMargin
    }

    val allImports = Imports.importedFiles(script)

    def importHeader = allImports.map(makeScript).mkString("\n")

    s"""
       |$importHeader
     """.stripMargin
  }

  def apply(workDirectory: File)(implicit newFile: NewFile, fileService: FileService) =
    new Project(workDirectory, Project.newREPL)

}

sealed trait CompileResult
case class ScriptFileDoesNotExists() extends CompileResult
sealed trait CompilationError extends CompileResult {
  def error: Throwable
}
case class ErrorInCode(error: ScalaREPL.CompilationError) extends CompilationError
case class ErrorInCompiler(error: Throwable) extends CompilationError

case class Compiled(result: ScalaREPL.Compiled) extends CompileResult {
  def eval =
    result.apply() match {
      case p: Puzzle ⇒ p
      case e         ⇒ throw new UserBadDataError(s"Script should end with a workflow (it ends with ${if (e == null) null else e.getClass}).")
    }
}

class Project(workDirectory: File, newREPL: (ConsoleVariables) ⇒ ScalaREPL) {

  def pluginsDirectory: File = workDirectory / "plugins"

  def plugins = pluginsDirectory.listFilesSafe

  def loadPlugins = PluginManager.load(plugins)

  def compile(script: File, args: Seq[String])(implicit services: Services): CompileResult = {
    if (!script.exists) ScriptFileDoesNotExists()
    else {
      def header =
        s"""${scriptsObjects(script)}
           |
           |def runOMSScript(): ${classOf[Puzzle].getCanonicalName} = {
           |import ${Project.uniqueName(script)}._imports._""".stripMargin

      def footer =
        s"""}
           |runOMSScript()""".stripMargin

      def compileContent =
        s"""$header
           |${script.content}
           |$footer""".stripMargin

      def compile(content: String, args: Seq[String]): CompileResult = {
        val loop = newREPL(ConsoleVariables(args, workDirectory))
        try {
          Option(loop.compile(content)) match {
            case Some(compiled) ⇒ Compiled(compiled)
            case None           ⇒ throw new InternalProcessingError("The compiler returned null instead of a compiled script, it may append if your script contains an unclosed comment block ('/*' without '*/').")
          }
        }
        catch {
          case ce: ScalaREPL.CompilationError ⇒
            def positionLens =
              ScalaREPL.CompilationError.errorMessages composeTraversal
                each composeLens
                ScalaREPL.ErrorMessage.position composePrism
                some

            def headerOffset = header.size + 1

            import ScalaREPL.ErrorPosition

            def adjusted =
              (positionLens composeLens ErrorPosition.line modify { _ - header.split("\n").size }) andThen
                (positionLens composeLens ErrorPosition.start modify { _ - headerOffset }) andThen
                (positionLens composeLens ErrorPosition.end modify { _ - headerOffset }) andThen
                (positionLens composeLens ErrorPosition.point modify { _ - headerOffset })

            ErrorInCode(adjusted(ce))
          case e: Throwable ⇒ ErrorInCompiler(e)
        }
        finally loop.close()
      }

      compile(compileContent, args)
    }
  }

  def scriptsObjects(script: File) = Project.scriptsObjects(script)

}
