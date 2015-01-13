/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external

import java.io.File
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.OS
import scala.collection.mutable.ListBuffer
import org.openmole.misc.tools.io.FileUtil._
import collection.mutable

object ExternalTask {
  val PWD = Prototype[String]("PWD")
}

trait ExternalTask extends Task {

  def inputFiles: Iterable[(Prototype[File], ExpandedString, Boolean)]
  def outputFiles: Iterable[(ExpandedString, Prototype[File])]
  def resources: Iterable[(File, ExpandedString, Boolean, OS)]

  protected case class ToPut(file: File, name: String, link: Boolean)
  protected case class ToGet(name: String, file: File)

  protected def listInputFiles(context: Context): Iterable[ToPut] =
    inputFiles.map {
      case (prototype, name, link) ⇒ ToPut(context(prototype), name.from(context), link)
    }

  protected def listResources(context: Context, tmpDir: File): Iterable[ToPut] = {
    val expanded =
      resources map { case v @ (_, name, _, _) ⇒ v.copy(_2 = name.from(context)) }

    val byLocation =
      expanded groupBy {
        case (_, name, _, _) ⇒ new File(tmpDir, name).getCanonicalPath
      }

    val selectedOS =
      byLocation.toList flatMap {
        case (_, values) ⇒
          values.find { case (_, _, _, os) ⇒ os.compatible }
      }

    selectedOS.map {
      case (file, name, link, _) ⇒ ToPut(file, name, link)
    }
  }

  protected def listOutputFiles(context: Context, localDir: File): (Context, Iterable[ToGet]) = {
    val files =
      outputFiles.map {
        case (name, prototype) ⇒
          val fileName = name.from(context)
          val file = new File(localDir, fileName)

          val fileVariable = Variable(prototype, file)
          ToGet(fileName, file) -> fileVariable
      }
    context ++ files.map { _._2 } -> files.map { _._1 }
  }

  private def copy(f: ToPut, to: File) = {
    to.getAbsoluteFile.getParentFile.mkdirs

    if (f.link) {
      to.createLink(f.file.getAbsolutePath)
      Some(to)
    }
    else {
      f.file.copy(to)
      to.applyRecursive { _.deleteOnExit }
      None
    }
  }

  def prepareInputFiles(context: Context, tmpDir: File, workDirPath: String = "") = {
    val workDir = new File(tmpDir, workDirPath)
    Set.empty[File] ++
      listInputFiles(context).flatMap(
        f ⇒ copy(f, new File(workDir, f.name))) ++
        listResources(context, tmpDir).flatMap(
          f ⇒ copy(f, new File(tmpDir, f.name)))
  }

  def fetchOutputFiles(context: Context, localDir: File, links: Set[File]): Context = {
    val (resultContext, outputFiles) = listOutputFiles(context, localDir)

    val usedFiles = outputFiles.map(
      f ⇒ {
        if (!f.file.exists) throw new UserBadDataError("Output file " + f.file.getAbsolutePath + " for task " + this.toString + " doesn't exist")
        f.file
      }).toSet

    links.foreach(_.delete)
    localDir.applyRecursive(f ⇒ f.delete, usedFiles)

    // This delete the dir only if it is empty
    localDir.delete
    resultContext
  }

  def withWorkDir[T](f: File ⇒ T): T = {
    val tmpDir = org.openmole.misc.workspace.Workspace.newDir("externalTask")
    val res =
      try f(tmpDir)
      catch {
        case e: Throwable ⇒
          tmpDir.recursiveDelete
          throw e
      }
    tmpDir.delete
    res
  }

}
