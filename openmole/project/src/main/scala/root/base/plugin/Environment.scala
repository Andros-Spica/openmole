package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.model, Misc.workspace, Misc.tools,
    Core.batch, provided(Core.serializer), Misc.sftpserver) settings (bundleType += "daemon")

  lazy val glite = OsgiProject("glite") dependsOn (Core.model, Misc.exception, Misc.updater, provided(Core.batch),
    Misc.workspace, provided(Libraries.scalaLang), Misc.fileService, gridscale) settings (
      libraryDependencies += Libraries.gridscaleGlite,
      libraryDependencies += Libraries.gridscaleDirac,
      libraryDependencies += Libraries.gridscaleHTTP)

  lazy val gridscale = OsgiProject("gridscale") dependsOn (Core.model, Misc.workspace, Misc.tools, Core.implementation,
    provided(Core.batch), Misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (Misc.exception, Misc.workspace, provided(Core.batch), gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscalePBS)

  lazy val sge = OsgiProject("sge") dependsOn (Misc.exception, Misc.workspace, provided(Core.batch), gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSGE)

  lazy val slurm = OsgiProject("slurm") dependsOn (Misc.exception, Misc.workspace, provided(Core.batch), gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSLURM)

  lazy val ssh = OsgiProject("ssh") dependsOn (Misc.exception, Misc.workspace, Misc.eventDispatcher, provided(Core.batch), gridscale) settings
    (libraryDependencies += Libraries.gridscaleSSH)

}
