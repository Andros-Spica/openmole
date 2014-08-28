package root.base.plugin

import root.base._
import root.Libraries._
import sbt._

object Method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.method")

  lazy val evolution = OsgiProject("evolution") dependsOn (Misc.exception, Core.implementation, mgo, Misc.workspace, Hook.file, plugin.Task.tools) //todo: other plugins have a dependency on MGO

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (Misc.exception, Core.implementation)

  lazy val stochastic = OsgiProject("stochastic") dependsOn (Core.implementation, plugin.Task.stat, Domain.distribution)

  lazy val abc = OsgiProject("abc") dependsOn (Misc.exception, Core.implementation, scalabc, Misc.workspace, Hook.file, plugin.Task.tools)

}
