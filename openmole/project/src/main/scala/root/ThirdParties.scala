package root

import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object ThirdParties extends Defaults {

  lazy val dir = file("third-parties")

  lazy val iceTar = OsgiProject("com.ice.tar") settings (bundleType += "runtime")

  lazy val scalaSwing = OsgiProject("org.scala-lang.scala-swing", exports = Seq("scala.swing.*", "scala.actors.*")) settings
    (libraryDependencies <+= scalaVersion { sV ⇒ "org.scala-lang" % "scala-swing" % sV })

  lazy val scopt = OsgiProject("com.github.scopt", exports = Seq("scopt.*")) settings (bundleType += "runtime")

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core", "libs"))

}