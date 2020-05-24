package lagompb

import com.lightbend.lagom.sbt.LagomImport.lagomScaladslTestKit
import sbt.Keys.libraryDependencies
import sbt.AutoPlugin
import sbt.Plugins
import sbt.plugins

/**
 * Dependencies required for testing
 */
object TestSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings =
    Seq(
      libraryDependencies ++= Seq(
        lagomScaladslTestKit,
        Dependencies.Test.scalaTest,
        Dependencies.Test.scalaMock,
        Dependencies.Test.akkaMultiNodeTeskit,
        Dependencies.Test.akkaTestkit,
        Dependencies.Test.akkaStreamTestkit,
        Dependencies.Test.akkaActorTeskitTyped,
        Dependencies.Compile.slickMigrationApi,
        Dependencies.Compile.postgresDriver,
        Dependencies.Compile.h2Driver,
        Dependencies.Compile.janino
      )
    )
}
