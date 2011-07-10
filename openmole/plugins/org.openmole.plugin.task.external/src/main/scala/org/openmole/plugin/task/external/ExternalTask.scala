/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

import org.openmole.core.implementation.data.Context._
import org.openmole.misc.tools.io.FileUtil

import org.openmole.core.implementation.tools.VariableExpansion._

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object ExternalTask {
  val PWD = new Prototype[String]("PWD", classOf[String])
}


abstract class ExternalTask(name: String) extends Task(name) {
 
  val inContextFiles = new ListBuffer[(IPrototype[File], String)]
  val inContextFileList = new ListBuffer[(IPrototype[Array[File]], IPrototype[Array[String]])]
  val inFileNames = new HashMap[File, String]

  val outFileNames = new ListBuffer[(IPrototype[File], String, Option[IPrototype[String]])]
  val outFileNamesFromVar = new ListBuffer[(IPrototype[File], IPrototype[String])]

  protected class ToPut(val file: File, val name: String)
  protected class ToGet(val name: String, val file: File)

  protected def listInputFiles(context: IContext): Iterable[ToPut] = {
    val file1 = inFileNames.map {
      entry => 
      val localFile = entry._1         
      new ToPut(localFile, expandData(context, entry._2))
    } 
    
    val file2 = inContextFiles.map { 
      p => 
      val f = context.value(p._1).getOrElse(throw new UserBadDataError("File supposed to be present in variable \"" + p._1.name + "\" at the beging of the task \"" + name + "\" and is not."))
      new ToPut(f, expandData(context, p._2))
    } 
    
    val file3 = inContextFileList.flatMap { 
      p => 
        val lstFile = context.value(p._1).get
        val lstName = context.value(p._2).get
        lstFile zip lstName map {
          case(f, name) => new ToPut(f, expandData(context, name)) 
        }
    }
    
    file1 ++ file2 ++ file3
  }


  protected def listOutputFiles(context: IContext, localDir: File): (IContext, Iterable[ToGet]) = {

    val file1 = outFileNames.map {
      case(fileProto, rawName, nameProtoOption) => 
        val filename = expandData(context, rawName)
        val fo = new File(localDir,filename)
        
        val fileVariable = new Variable(fileProto, fo)
        
        (nameProtoOption match {
            case None => fileVariable :: Nil
            case Some(nameProto) => fileVariable :: new Variable(nameProto, filename) :: Nil
          }) -> new ToGet(filename, fo)
    }

    val file2 = outFileNamesFromVar map { 
      case(fileProto, fileNameProto) => 
        val filename = context.value(fileNameProto).getOrElse(throw new UserBadDataError("Variable containing the output file name should exist in the context at the end of the task" + name))
        val fo = new File(localDir, filename)
        new Variable(fileProto, fo) -> new ToGet(filename, fo)
    }

    (context ++ file1.flatMap{_._1} ++ file2.map{_._1}) -> (file1.map{_._2} ++ file2.map{_._2})
  }

  def addInput(fileList: IPrototype[Array[File]], names: IPrototype[Array[String]]): this.type = {
    inContextFileList += ((fileList, names))
    super.addInput(fileList)
    super.addInput(names)
    this
  }

  def addInput(fileProt: IPrototype[File], name: String): this.type = {
    inContextFiles += ((fileProt, name))
    super.addInput(fileProt)
    this
  }

  def addOutput(fileName: String, v: IPrototype[File]): this.type = {
    outFileNames += ((v, fileName, None))
    addOutput(v)
    this
  }

  def addOutput(fileName: String, v: IPrototype[File], varFileName: IPrototype[String]): this.type = {
    outFileNames += ((v, fileName, Some(varFileName)))
    addOutput(varFileName)
    addOutput(v)
    this
  }

  def addOutput(varFileName: IPrototype[String], v: IPrototype[File]): this.type = {
    addOutput(v)
    outFileNamesFromVar += v -> varFileName
    this
  }

  def addResource(file: File, name: String): this.type = {
    inFileNames.put(file, name)
    this
  }

  def addResource(file: File): this.type = addResource(file, file.getName)
    
  def addResource(location: String): this.type = addResource(new File(location))

  def addResource(location: String, name: String): this.type =  addResource(new File(location), name)
  
}
