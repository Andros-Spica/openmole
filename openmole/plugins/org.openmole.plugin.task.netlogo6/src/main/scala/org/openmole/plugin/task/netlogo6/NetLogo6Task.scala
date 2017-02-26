/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.task.netlogo6

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.Val
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.netlogo.NetLogoTask.Workspace
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.tool.netlogo6._

object NetLogo6Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo6
  }

  implicit def isTask: InputOutputBuilder[NetLogo6Task] = InputOutputBuilder(NetLogo6Task.config)
  implicit def isExternal: ExternalBuilder[NetLogo6Task] = ExternalBuilder(NetLogo6Task.external)

  implicit def isBuilder = new NetLogoTaskBuilder[NetLogo6Task] {
    override def netLogoInputs = NetLogo6Task.netLogoInputs
    override def netLogoArrayOutputs = NetLogo6Task.netLogoArrayOutputs
    override def netLogoOutputs = NetLogo6Task.netLogoOutputs
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[String],
    seed:              OptionalArgument[Val[Int]] = None
  )(implicit name: sourcecode.Name): NetLogo6Task =
    withDefaultArgs(
      workspace = Workspace(script = script, workspace = workspace.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.option.toSeq: _*),
        resources += workspace
      )

  def file(
    script:            File,
    launchingCommands: Seq[String],
    seed:              OptionalArgument[Val[Int]] = None
  )(implicit name: sourcecode.Name): NetLogo6Task =
    withDefaultArgs(
      workspace = Workspace(script = script.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.option.toSeq: _*),
        resources += script
      )

  def apply(
    script:            File,
    launchingCommands: Seq[String],
    embedWorkspace:    Boolean                    = false,
    seed:              OptionalArgument[Val[Int]] = None
  )(implicit name: sourcecode.Name): NetLogo6Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed)
    else file(script, launchingCommands, seed)

  private def withDefaultArgs(
    workspace:         NetLogoTask.Workspace,
    launchingCommands: Seq[String],
    seed:              Option[Val[Int]]
  )(implicit name: sourcecode.Name) =
    NetLogo6Task(
      config = InputOutputConfig(),
      external = External(),
      netLogoInputs = Vector.empty,
      netLogoOutputs = Vector.empty,
      netLogoArrayOutputs = Vector.empty,
      workspace = workspace,
      launchingCommands = launchingCommands,
      seed = seed
    )

}

@Lenses case class NetLogo6Task(
    config:              InputOutputConfig,
    external:            External,
    netLogoInputs:       Vector[(Val[_], String)],
    netLogoOutputs:      Vector[(String, Val[_])],
    netLogoArrayOutputs: Vector[(String, Int, Val[_])],
    workspace:           NetLogoTask.Workspace,
    launchingCommands:   Seq[String],
    seed:                Option[Val[Int]]
) extends NetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo6Task.factory
}

