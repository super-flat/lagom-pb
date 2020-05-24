package lagompb

import lagompb.Dependencies.Versions
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.AutoPlugin
import sbt.CrossVersion
import sbt.Developer
import sbt.Plugins
import sbt.compilerPlugin
import sbt.plugins
import sbt.url
import sbt._
import scoverage.ScoverageKeys.coverageFailOnMinimum
import scoverage.ScoverageKeys.coverageMinimum

object CommonSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings = Seq(
    scalaVersion := Versions.scala213,
    organization := "io.superflat",
    organizationName := "",
    organizationHomepage := Some(url("https://superflat.io/")),
    homepage := Some(url("https://github.com/super-flat/lagom-pb")),
    developers += Developer(
      "contributors",
      "Contributors",
      "",
      url("https://github.com/super-flat/lagom-pb/graphs/contributors")
    ),
    description := "lagompb - Scala shared code for lagom development in lagom using protobuf.\n",
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

  override def projectSettings = Seq(
    javacOptions := Seq(
      "-source",
      "1.8",
      "-target",
      "1.8",
      "-Xlint:-options",
      "-encoding",
      "UTF-8",
      "-XDignore.symbol.file"
    ),
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation",
      "-Xlint",
      "-P:silencer:checkUnused",
      "-P:silencer:globalFilters=Unused import;Marked as deprecated in proto file",
      // generated code for methods/fields marked 'deprecated'
      "-P:silencer:globalFilters=Marked as deprecated in proto file",
      // generated scaladoc sometimes has this problem
      "-P:silencer:globalFilters=unbalanced or unclosed heading",
      // deprecated in 2.13, but used as long as we support 2.12
      "-P:silencer:globalFilters=Use `scala.jdk.CollectionConverters` instead",
      "-P:silencer:globalFilters=Use LazyList instead of Stream",
      // ignore imports in templates
      "-P:silencer:pathFilters=.*.txt"
    ),
    libraryDependencies ++= Seq(
      compilerPlugin(("com.github.ghik" % "silencer-plugin" % Versions.silencerVersion).cross(CrossVersion.full)),
      ("com.github.ghik" % "silencer-lib" % Versions.silencerVersion % Provided).cross(CrossVersion.full)
    ),
    resolvers ++= Seq(
      Resolver.jcenterRepo
    ),
    scalafmtOnCompile := true
  )
}
