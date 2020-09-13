import com.lightbend.lagom.sbt.LagomImport.lagomScaladslTestKit
import sbt.Keys.libraryDependencies
import sbt.{plugins, AutoPlugin, Plugins}

/**
 * Dependencies required for testing
 */
object TestSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings =
    Seq(
      libraryDependencies ++= Seq(
        lagomScaladslTestKit,
        Dependencies.Test.ScalaTest,
        Dependencies.Test.ScalaMock,
        Dependencies.Test.AkkaMultiNodeTestkit,
        Dependencies.Test.AkkaTestkit,
        Dependencies.Test.AkkaStreamTestkit,
        Dependencies.Test.AkkaActorTestkitTyped,
        Dependencies.Compile.postgresDriver,
        Dependencies.Compile.H2Driver
      )
    )
}
