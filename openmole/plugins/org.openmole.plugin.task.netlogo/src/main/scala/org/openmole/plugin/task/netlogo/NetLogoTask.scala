/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.task.netlogo

import java.util.AbstractCollection

import org.openmole.core.context.{ Context, Prototype, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.ExpandedString
import org.openmole.core.tools.io.Prettifier
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.plugin.task.external._
import org.openmole.tool.cache.Cache
import org.openmole.tool.random.RandomProvider

object NetLogoTask {
  case class Workspace(script: String, workspace: OptionalArgument[String] = None)
}

trait NetLogoTask extends Task {

  def workspace: NetLogoTask.Workspace
  def launchingCommands: Seq[String]
  def netLogoInputs: Seq[(Prototype[_], String)]
  def netLogoOutputs: Iterable[(String, Prototype[_])]
  def netLogoArrayOutputs: Iterable[(String, Int, Prototype[_])]
  def netLogoFactory: NetLogoFactory
  def seed: Option[Prototype[Int]]
  def external: External

  private def wrapError[T](msg: String)(f: ⇒ T): T =
    try f
    catch {
      case e: Throwable ⇒
        throw new UserBadDataError(s"$msg:\n" + e.stackStringWithMargin)
    }

  val expandedCommands = Cache(launchingCommands.map(ExpandedString(_)))

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = external.withWorkDir(executionContext) { tmpDir ⇒
    val workDir =
      workspace.workspace.option match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ tmpDir / d
      }

    val preparedContext = external.prepareInputFiles(context, external.relativeResolver(tmpDir))

    val script = workDir / workspace.script
    val netLogo = netLogoFactory()
    withClassLoader(netLogo.getNetLogoClassLoader) {
      try {
        wrapError(s"Error while opening the file $script") {
          netLogo.open(script.getAbsolutePath)
        }

        def executeNetLogo(cmd: String) = wrapError(s"Error while executing command $cmd") {
          netLogo.command(cmd)
        }

        seed.foreach { s ⇒ executeNetLogo(s"random-seed ${context(s)}") }

        for (inBinding ← netLogoInputs) {
          val v = preparedContext(inBinding._1) match {
            case x: String ⇒ '"' + x + '"'
            case x         ⇒ x.toString
          }
          executeNetLogo("set " + inBinding._2 + " " + v)
        }

        for (cmd ← expandedCommands()) executeNetLogo(cmd.from(context))

        val contextResult =
          external.fetchOutputFiles(preparedContext, external.relativeResolver(workDir)) ++ netLogoOutputs.map {
            case (name, prototype) ⇒
              try {
                val outputValue = netLogo.report(name)
                if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Prototype[Any]], outputValue)
                else {
                  val netLogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
                  netLogoArrayToVariable(netLogoCollection, prototype)
                }
              }
              catch {
                case e: Throwable ⇒
                  throw new UserBadDataError(
                    s"Error when fetching netlogo output $name in variable $prototype:\n" + e.stackStringWithMargin
                  )
              }
          } ++ netLogoArrayOutputs.map {
            case (name, column, prototype) ⇒
              try {
                val netLogoCollection = netLogo.report(name)
                val outputValue = netLogoCollection.asInstanceOf[AbstractCollection[Any]].toArray()(column)
                if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Prototype[Any]], outputValue)
                else netLogoArrayToVariable(outputValue.asInstanceOf[AbstractCollection[Any]], prototype)
              }
              catch {
                case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when fetching column $column of netlogo output $name in variable $prototype")
              }
          }

        external.checkAndClean(this, contextResult, tmpDir)
        contextResult
      }
      finally netLogo.dispose
    }
  }

  private def withClassLoader[T](classLoader: ClassLoader)(f: ⇒ T): T = {
    val threadClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(classLoader)
    try f
    finally Thread.currentThread().setContextClassLoader(threadClassLoader)
  }

  def netLogoArrayToVariable(netlogoCollection: AbstractCollection[Any], prototype: Prototype[_]) = {
    val arrayType = prototype.`type`.runtimeClass.getComponentType
    val array = java.lang.reflect.Array.newInstance(arrayType, netlogoCollection.size)
    val it = netlogoCollection.iterator
    for (i ← 0 until netlogoCollection.size) {
      val v = it.next
      try java.lang.reflect.Array.set(array, i, v)
      catch {
        case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when adding a variable of type ${v.getClass} in an array of ${arrayType}")
      }
    }
    Variable(prototype.asInstanceOf[Prototype[Any]], array)
  }

}
