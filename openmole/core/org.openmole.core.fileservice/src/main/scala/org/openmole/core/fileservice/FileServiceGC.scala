/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.fileservice

import java.io.File

import org.openmole.core.threadprovider.IUpdatable
import scala.ref.WeakReference
import collection.JavaConverters._
import org.openmole.tool.file._

class FileServiceGC(fileService: WeakReference[FileService]) extends IUpdatable {

  override def update: Boolean =
    fileService.get match {
      case Some(fileService) ⇒
        def invalidateArchive =
          for {
            file ← fileService.archiveCache.asMap().keySet().asScala.toSeq
            if (!new File(file).exists)
          } yield file

        fileService.archiveCache.invalidateAll(invalidateArchive.asJava)

        def invalidateHash =
          for {
            file ← fileService.hashCache.asMap().keySet().asScala.toSeq
            if !new File(file).exists
          } yield file

        fileService.hashCache.invalidateAll(invalidateHash.asJava)

        fileService.deleteEmpty.synchronized {
          def deleteEmpty(files: Vector[File]): Vector[File] = {
            val (empty, nonEmpty) = files.partition(f ⇒ !f.exists() || f.directoryIsEmpty)
            empty.foreach { f ⇒ f.recursiveDelete }
            if (!empty.isEmpty) deleteEmpty(nonEmpty) else nonEmpty
          }

          val nonEmpty = deleteEmpty(fileService.deleteEmpty.toVector)
          fileService.deleteEmpty.clear()
          fileService.deleteEmpty ++= nonEmpty
        }

        true
      case None ⇒ false
    }
}
