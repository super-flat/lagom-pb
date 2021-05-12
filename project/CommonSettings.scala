import Dependencies.Versions
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt._

object CommonSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings =
    Seq(
      scalaVersion := Versions.Scala213,
      organization := "io.superflat",
      organizationName := "Super Flat",
      startYear := Some(2020),
      organizationHomepage := Some(url("https://superflat.io/")),
      homepage := Some(url("https://github.com/super-flat/lagom-pb")),
      scmInfo := Some(ScmInfo(url("https://github.com/super-flat/lagom-pb"), "git@github.com:super-flat/lagom-pb.git")),
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      developers += Developer(
        "contributors",
        "Contributors",
        "",
        url("https://github.com/super-flat/lagom-pb/graphs/contributors")),
      description := "lagom-pb - Scala shared code for lagom development in lagom using protobuf.\n")

  override def projectSettings =
    Seq(
      javacOptions := Seq(
        "-source",
        "1.8",
        "-target",
        "1.8",
        "-Xlint:-options",
        "-encoding",
        "UTF-8",
        "-XDignore.symbol.file"),
      scalacOptions ++= Seq(
        "-feature",
        "-unchecked",
        "-Xfatal-warnings",
        "-deprecation",
        // linter option
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
        "-P:silencer:pathFilters=.*.txt"),
      resolvers ++= Seq(Resolver.jcenterRepo, Resolver.sonatypeRepo("public"), Resolver.sonatypeRepo("snapshots")),
      libraryDependencies ++= Seq(
        compilerPlugin(("com.github.ghik" % "silencer-plugin" % Versions.SilencerVersion).cross(CrossVersion.full)),
        ("com.github.ghik" % "silencer-lib" % Versions.SilencerVersion % Provided).cross(CrossVersion.full)),
      scalafmtOnCompile := true)
}
