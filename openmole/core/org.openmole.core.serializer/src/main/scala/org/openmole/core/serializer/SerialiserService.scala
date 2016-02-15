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

package org.openmole.core.serializer

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import com.thoughtworks.xstream.core.ClassLoaderReference
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver
import org.openmole.core.fileservice.FileDeleter
import org.openmole.tool.file._
import org.openmole.core.serializer.converter._
import java.util.concurrent.locks.{ ReentrantReadWriteLock, ReadWriteLock }
import org.openmole.core.workspace.Workspace
import org.openmole.tool.logger.Logger
import org.openmole.tool.stream
import org.openmole.tool.tar._
import org.openmole.tool.lock._
import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{ FileInjection, FileSerialisation }

object SerialiserService extends Logger {

  private[serializer] def buildXStream =
    new XStream(null, new BinaryStreamDriver(), new ClassLoaderReference(SerialiserService.getClass.getClassLoader))

  private val lock = new ReentrantReadWriteLock
  private val xStreamOperations = ListBuffer.empty[(XStream ⇒ _)]

  private val xstream = buildXStream
  private val content = "content.xml"

  private trait Initialized extends Factory {
    override def initialize(t: T) = lock.read {
      for {
        op ← xStreamOperations
      } op(t.xStream)
      t
    }
  }

  private val fileSerialisation = new Factory with Initialized {
    type T = Serialiser with FileSerialisation
    def make = new Serialiser with FileSerialisation
  }

  private val pluginAndFileListingFactory = new Factory with Initialized {
    type T = Serialiser with PluginAndFilesListing
    def make = new Serialiser with PluginAndFilesListing
  }

  private val deserialiserWithFileInjectionFactory = new Factory with Initialized {
    type T = Serialiser with FileInjection
    def make = new Serialiser with FileInjection
  }

  private def xStreams =
    xstream ::
      pluginAndFileListingFactory.instantiated.map(_.xStream) :::
      deserialiserWithFileInjectionFactory.instantiated.map(_.xStream)

  def register(op: XStream ⇒ Unit) = lock.write {
    xStreamOperations += op
    xStreams.foreach(op)
  }

  def deserialise[T](file: File): T = lock.read {
    val is = new FileInputStream(file)
    try deserialise(is)
    finally is.close
  }

  def deserialise[T](is: InputStream): T = lock.read(xstream.fromXML(is).asInstanceOf[T])

  def deserialiseAndExtractFiles[T](file: File, extractDir: File = Workspace.tmpDir): T = {
    val tis = new TarInputStream(file.bufferedInputStream)
    try deserialiseAndExtractFiles(tis, extractDir)
    finally tis.close
  }

  def deserialiseAndExtractFiles[T](tis: TarInputStream, extractDir: File): T = lock.read {
    val archiveExtractDir = extractDir.newDir("archiveExtraction")
    tis.extract(archiveExtractDir)
    val fileReplacement = fileSerialisation.exec(_.deserialiseFileReplacements(archiveExtractDir, extractDir))
    val contentFile = new File(archiveExtractDir, content)
    val obj = deserialiseReplaceFiles[T](contentFile, fileReplacement)
    archiveExtractDir.recursiveDelete
    obj
  }

  def serialiseAndArchiveFiles(obj: Any, f: File): Unit = {
    val os = new TarOutputStream(f.bufferedOutputStream())
    try serialiseAndArchiveFiles(obj, os)
    finally os.close
  }

  def serialiseAndArchiveFiles(obj: Any, tos: TarOutputStream): Unit = lock.read {
    Workspace.withTmpFile { objSerial ⇒
      serialise(obj, objSerial)
      tos.addFile(objSerial, content)
    }
    val serializationResult = pluginsAndFiles(obj)
    fileSerialisation.exec(_.serialiseFiles(serializationResult.files, tos))
  }

  def pluginsAndFiles(obj: Any) = pluginAndFileListingFactory.exec(_.list(obj))

  def deserialiseReplaceFiles[T](file: File, files: PartialFunction[String, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserialiseReplaceFiles[T](is, files)
    finally is.close
  }

  def deserialiseReplaceFiles[T](is: InputStream, files: PartialFunction[String, File]): T =
    lock.read(deserialiserWithFileInjectionFactory.exec {
      serializer ⇒
        serializer.injectedFiles = files
        serializer.fromXML[T](is)
    })

  def serialise(obj: Any) = lock.read(xstream.toXML(obj))

  def serialise(obj: Any, os: OutputStream) = lock.read(xstream.toXML(obj, os))

  def serialise(obj: Any, file: File): Unit = lock.read {
    val os = file.bufferedOutputStream()
    try serialise(obj, os)
    finally os.close
  }

}
