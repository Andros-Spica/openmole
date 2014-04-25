/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.batch.storage

import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.refresh._
import org.openmole.core.serializer._
import org.openmole.misc.filedeleter._
import org.openmole.misc.workspace._
import com.db4o.ObjectContainer
import fr.iscpif.gridscale.FileType
import java.io._
import org.openmole.misc.tools.service.Logger

object StorageService extends Logger

import StorageService.Log._

trait StorageService extends BatchService with Storage {

  def remoteStorage: RemoteStorage
  def clean(implicit token: AccessToken, objectContainer: ObjectContainer)

  def url: URI
  lazy val id = url.toString

  @transient lazy val serializedRemoteStorage = {
    val file = Workspace.newFile("remoteStorage", ".xml")
    FileDeleter.deleteWhenGarbageCollected(file)
    SerialiserService.serialiseAndArchiveFiles(remoteStorage, file)
    file
  }

  @transient protected var baseSpaceVar: Option[String] = None

  def persistentDir(implicit token: AccessToken, objectContainer: ObjectContainer): String
  def tmpDir(implicit token: AccessToken): String
  def baseDir(implicit token: AccessToken): String = synchronized {
    baseSpaceVar match {
      case Some(s) ⇒ s
      case None ⇒
        val rootPath = mkRootDir
        val basePath = child(rootPath, baseDirName)
        if (!exists(basePath)) makeDir(basePath)
        initialise(basePath)
        baseSpaceVar = Some(basePath)
        basePath
    }
  }

  protected def initialise(basePath: String)(implicit token: AccessToken) = {}

  protected def mkRootDir(implicit token: AccessToken): String = synchronized {
    root.split("/").toList.filterNot(_.isEmpty).foldLeft("/") {
      (path, file) ⇒
        val childPath = child(path, file)
        try makeDir(childPath)
        catch {
          case e: Throwable ⇒ logger.log(FINEST, "Error creating base directory " + root + e)
        }
        childPath
    }
  }

  override def toString: String = id

  def exists(path: String)(implicit token: AccessToken): Boolean = token.synchronized { super.exists(path) }
  def listNames(path: String)(implicit token: AccessToken): Seq[String] = token.synchronized { super.listNames(path) }
  def list(path: String)(implicit token: AccessToken): Seq[(String, FileType)] = token.synchronized { super.list(path) }
  def makeDir(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.makeDir(path) }
  def rmDir(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.rmDir(path) }
  def rmFile(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.rmFile(path) }
  def mv(from: String, to: String)(implicit token: AccessToken) = token.synchronized { super.mv(from, to) }
  def openInputStream(path: String)(implicit token: AccessToken): InputStream = token.synchronized { super.openInputStream(path) }
  def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = token.synchronized { super.openOutputStream(path) }

  def upload(src: File, dest: String)(implicit token: AccessToken) = token.synchronized { super.upload(src, dest) }
  def uploadGZ(src: File, dest: String)(implicit token: AccessToken) = token.synchronized { super.uploadGZ(src, dest) }
  def download(src: String, dest: File)(implicit token: AccessToken) = token.synchronized { super.download(src, dest) }
  def downloadGZ(src: String, dest: File)(implicit token: AccessToken) = token.synchronized { super.downloadGZ(src, dest) }

  def baseDirName = Workspace.preference(Workspace.uniqueID) + '/'

  def backgroundRmFile(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, true)

}
