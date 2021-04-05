import com.lightbend.lagom.sbt.LagomImport.lagomScaladslTestKit
import sbt.{plugins, AutoPlugin, Plugins}
import sbt.Keys.libraryDependencies

/**
 * Dependencies required for testing
 */
object TestSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings =
    Seq(
      libraryDependencies ++= Seq(lagomScaladslTestKit) ++ Dependencies.TestJars
    )
}
