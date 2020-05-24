package lagompb

import sbt.Keys.publishArtifact
import sbt.Keys.skip
import sbt.Keys._
import sbt.AutoPlugin
import sbt.plugins
import sbt._

/**
 * For projects that are not to be published.
 */
object NoPublish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings = Seq(
    publishArtifact := false,
    skip in publish := true
  )
}

object Publish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def trigger = allRequirements

  def getEnvWithWarning(key: String): String = {
    sys.env.get(key) match {
      case Some(value) => value
      case None =>
        // scalastyle:off println
        println(s"**** WARNING: ENV VAR '$key' MISSING")
        // scalastyle:on println
        ""
    }
  }

  override def projectSettings = Seq(
    version := sys.env.getOrElse("VERSION", "development"),
    isSnapshot := !version.value.matches("^\\d+\\.\\d+\\.\\d+$"),
    resolvers += Resolver.jcenterRepo,
    publishArtifact := true,
    Test / publishArtifact := false
  )
}
