package root

import libraries._
import thirdparties._
import sbt._
import Keys._

import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

package object application extends Defaults {
  override lazy val org = organization := "org.openmole.ui"
  implicit val dir = file("application")
  lazy val all = Project("application", file("application")) aggregate (plugins, openmoleui,
    openmolePlugins, openmoleGuiPlugins, openmoleResources, openMoleDB, openmoleRuntime, openmoleDaemon)

  private val openmoleUILibDependencies = libraryDependencies ++= Seq(
    "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.contenttype" % "3.4.200.v20120523-2004" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.jobs" % "3.5.300.v20120912-155018" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.runtime" % "3.8.0.v20120912-155025" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.common" % "3.6.100.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.registry" % "3.5.200.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.preferences" % "3.5.1.v20121031-182809" intransitive (),
    "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944" intransitive ()
  )

  private lazy val pluginDependencies = libraryDependencies <++= (version) { v ⇒
    Seq(
      "org.openmole.core" %% "org.openmole.misc.sftpserver" % v,
      "org.openmole.core" %% "org.openmole.misc.logging" % v,
      "org.openmole.core" %% "org.openmole.core.model" % v,
      "org.openmole.core" %% "org.openmole.core.implementation" % v,
      "org.openmole.web" %% "org.openmole.web.core" % v,
      "org.openmole.core" %% "org.openmole.misc.workspace" % v,
      "org.openmole.core" %% "org.openmole.misc.replication" % v,
      "org.openmole.core" %% "org.openmole.misc.exception" % v,
      "org.openmole.core" %% "org.openmole.misc.tools" % v,
      "org.openmole.core" %% "org.openmole.misc.eventdispatcher" % v,
      "org.openmole.core" %% "org.openmole.misc.pluginmanager" % v,
      "org.openmole.core" %% "org.openmole.misc.osgi" % v,
      "org.openmole.core" %% "org.openmole.misc.updater" % v,
      "org.openmole.core" %% "org.openmole.misc.fileservice" % v,
      "org.openmole.core" %% "org.openmole.misc.filecache" % v,
      "org.openmole.core" %% "org.openmole.misc.filedeleter" % v,
      "org.openmole.core" %% "org.openmole.misc.hashservice" % v,

      "org.openmole.core" %% "org.openmole.core.batch" % v,

      "org.openmole" %% "uk.com.robustit.cloning" % v intransitive (),
      "org.openmole" %% "com.ibm.icu" % v intransitive (),
      "org.openmole" %% "fr.iscpif.gridscale" % v intransitive (),
      "org.openmole" %% "org.apache.commons.pool" % v intransitive (),
      "org.openmole" %% "org.apache.commons.exec" % v intransitive (),
      "org.openmole" %% "org.gnu.crypto" % v intransitive (),
      "org.openmole" %% "org.joda.time" % v intransitive (),
      "org.openmole" %% "org.scala-lang.scala-library" % v intransitive (),
      "org.openmole" %% "org.jasypt.encryption" % v intransitive (),
      "org.openmole" %% "org.apache.commons.configuration" % v intransitive (),
      "org.openmole" %% "org.objenesis" % v intransitive (),
      "org.openmole" %% "com.github.scopt" % v intransitive (),
      "org.openmole" %% "org.apache.commons.logging" % v intransitive (),
      "org.openmole" %% "net.sourceforge.jline" % v intransitive (),
      "org.openmole" %% "org.apache.log4j" % v intransitive (),
      "org.openmole" %% "org.apache.ant" % v intransitive ()
    )
  }

  private lazy val openmolePluginDependencies = libraryDependencies <++= (version) {
    (v) ⇒
      {
        def sbtPluginTemplate(subId: String) = "org.openmole.core" %% ("org.openmole.plugin." + subId) % v intransitive ()
        Seq(sbtPluginTemplate("tools.groovy"),
          sbtPluginTemplate("environment.gridscale"),
          sbtPluginTemplate("environment.glite"),
          sbtPluginTemplate("environment.desktopgrid"),
          sbtPluginTemplate("environment.ssh"),
          sbtPluginTemplate("environment.pbs"),
          sbtPluginTemplate("grouping.onvariable"),
          sbtPluginTemplate("grouping.batch"),
          sbtPluginTemplate("task.netlogo"),
          sbtPluginTemplate("task.netlogo4"),
          sbtPluginTemplate("task.netlogo5"),
          sbtPluginTemplate("task.systemexec"),
          sbtPluginTemplate("task.groovy"),
          sbtPluginTemplate("task.scala"),
          sbtPluginTemplate("task.code"),
          sbtPluginTemplate("task.external"),
          sbtPluginTemplate("task.template"),
          sbtPluginTemplate("task.stat"),
          sbtPluginTemplate("domain.modifier"),
          sbtPluginTemplate("domain.file"),
          sbtPluginTemplate("domain.collection"),
          sbtPluginTemplate("sampling.csv"),
          sbtPluginTemplate("sampling.lhs"),
          sbtPluginTemplate("sampling.combine"),
          sbtPluginTemplate("sampling.filter"),
          sbtPluginTemplate("domain.range"),
          sbtPluginTemplate("domain.bounded"),
          sbtPluginTemplate("domain.relative"),
          sbtPluginTemplate("domain.distribution"),
          sbtPluginTemplate("profiler.csvprofiler"),
          sbtPluginTemplate("hook.file"),
          sbtPluginTemplate("hook.display"),
          sbtPluginTemplate("source.file"),
          sbtPluginTemplate("method.evolution"),
          sbtPluginTemplate("method.sensitivity"),
          sbtPluginTemplate("builder.base"),
          sbtPluginTemplate("builder.evolution"),
          sbtPluginTemplate("builder.stochastic"),
          "org.openmole" %% "au.com.bytecode.opencsv" % v intransitive (),
          "org.openmole" %% "ccl.northwestern.edu.netlogo5" % "5.0.3" intransitive (),
          "org.openmole" %% "ccl.northwestern.edu.netlogo4" % "4.1.3" intransitive (),
          "org.openmole" %% "fr.iscpif.mgo" % v intransitive ()
        )
      }
  }

  private lazy val openmoleGuiPluginDependencies = libraryDependencies <++= (version) {
    (v) ⇒
      {
        def sbtPluginTemplate(subArtifact: String) = ("org.openmole.ide" %% ("org.openmole.ide.plugin." + subArtifact) % v) intransitive ()
        Seq(sbtPluginTemplate("task.groovy"),
          sbtPluginTemplate("task.exploration"),
          sbtPluginTemplate("task.netlogo"),
          sbtPluginTemplate("task.systemexec"),
          sbtPluginTemplate("task.moletask"),
          sbtPluginTemplate("task.stat"),
          sbtPluginTemplate("domain.range"),
          sbtPluginTemplate("domain.collection"),
          sbtPluginTemplate("domain.modifier"),
          sbtPluginTemplate("domain.file"),
          sbtPluginTemplate("domain.distribution"),
          sbtPluginTemplate("sampling.csv"),
          sbtPluginTemplate("sampling.combine"),
          sbtPluginTemplate("sampling.lhs"),
          sbtPluginTemplate("environment.local"),
          sbtPluginTemplate("environment.glite"),
          sbtPluginTemplate("environment.pbs"),
          sbtPluginTemplate("environment.desktopgrid"),
          sbtPluginTemplate("environment.ssh"),
          sbtPluginTemplate("method.sensitivity"),
          sbtPluginTemplate("groupingstrategy"),
          sbtPluginTemplate("misc.tools"),
          sbtPluginTemplate("hook.display"),
          sbtPluginTemplate("source.file"),
          sbtPluginTemplate("builder.base"),
          sbtPluginTemplate("hook.file"),
          "org.openmole.ide" %% "org.openmole.ide.osgi.netlogo" % v intransitive (),
          "org.openmole.ide" %% "org.openmole.ide.osgi.netlogo4" % v intransitive (),
          "org.openmole.ide" %% "org.openmole.ide.osgi.netlogo5" % v intransitive ()
        )
      }
  }

  lazy val openmoleui = OsgiProject("org.openmole.ui", singleton = true) settings (openmoleUILibDependencies) dependsOn
    (web.core, base.misc.workspace, base.misc.replication, base.misc.exception, base.misc.tools, base.misc.eventDispatcher,
      base.misc.pluginManager, jodaTime, scalaLang, jasypt, apache.config, objenesis, base.core.implementation, robustIt,
      scopt, base.core.batch, gui.core.implementation, base.misc.sftpserver, base.misc.logging, jline, apache.logging,
      apache.ant)

  lazy val plugins = AssemblyProject("package", "plugins",
    Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") })
  ) settings (openmoleUILibDependencies, pluginDependencies,
      libraryDependencies <++= (version) { v ⇒
        Seq(
          "org.openmole.ide" %% "org.openmole.ide.core.implementation" % v,
          "org.openmole.ide" %% "org.openmole.ide.misc.visualization" % v,
          "org.openmole" %% "de.erichseifert.gral" % v intransitive (),
          "org.openmole.ui" %% "org.openmole.ui" % v exclude ("org.eclipse.equinox", "*")
        )
      }, dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openmolePlugins = AssemblyProject("package", "openmole-plugins") settings (openmolePluginDependencies,
    dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openmoleGuiPlugins = AssemblyProject("package", "openmole-plugins-gui") settings (openmoleGuiPluginDependencies,
    dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openmoleResources = AssemblyProject("package", "") settings
    (resourceDirectory <<= baseDirectory / "resources", copyResTask, assemble <<= assemble dependsOn (resourceAssemble),
      dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openMoleDB = AssemblyProject("package", "dbserver/lib") settings (libraryDependencies <+= (version)
    { v ⇒ "org.openmole.core" %% "org.openmole.runtime.dbserver" % v },
    copyResTask, resourceDirectory <<= baseDirectory / "db-resources", assemble <<= assemble dependsOn (resourceAssemble),
    resourceOutDir := Option("dbserver/bin"))

  lazy val openmoleRuntime = AssemblyProject("runtime", "plugins",
    Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") })) settings (openmoleUILibDependencies, pluginDependencies, copyResTask,
      resourceDirectory <<= baseDirectory / "resources", libraryDependencies <+= (version) { "org.openmole.core" %% "org.openmole.runtime.runtime" % _ },
      assemble <<= assemble dependsOn resourceAssemble, resourceOutDir := Option("."))

  lazy val openmoleDaemon = AssemblyProject("daemon", "plugins") settings (copyResTask, resourceDirectory <<= baseDirectory / "resources",
    libraryDependencies <+= (version) { "org.openmole.core" %% "org.openmole.runtime.daemon" % _ }, assemble <<= assemble dependsOn resourceAssemble,
    resourceOutDir := Option("."))

  lazy val rpm = AssemblyProject("package", "packages") settings (packagerSettings: _*) settings (
    maintainer in Debian := "Romain Reuillon <romain@reuillon.org>",
    maintainer in Rpm <<= maintainer in Debian,
    packageSummary in Linux := "Open MOdeL Experiment workflow engine",
    packageDescription in Rpm := """This package contains the OpenMole executable, an easy to use system for massively parrelel computation.""",
    packageDescription in Debian <<= packageDescription in Rpm,
    linuxPackageMappings <+= (target in Linux) map { (ct: File) ⇒
      println(ct)
      val src = ct / "assembly"
      val dest = "/opt/openmole"
      packageMapping(
        (for {
          path ← (src ***).get
        } yield path -> path.toString.replaceFirst(src.toString, dest)): _*
      ) withUser "root" withGroup "root" withPerms "0755"
    },
    name in Rpm := "OpenMOLE",
    rpmRelease := "1",
    rpmVendor := "iscpif",
    rpmUrl := Some("http://www.openmole.org/"),
    rpmLicense := Some("AGPL3"),
    version in Rpm <<= (version) { v ⇒ v.replace("-", ".") },
    debianPackageDependencies := Seq("openjdk-7-jdk"),
    rpmPrerequisites := Seq("java-1.7.0-openjdk"),
    name in Debian := "OpenMOLE",
    version in Debian <<= (version) { v ⇒ v.replace("-", ".") }
  )
}