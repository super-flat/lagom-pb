import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt.{plugins, AutoPlugin, Plugins}
import sbt.Keys.{dependencyOverrides, libraryDependencies}

/**
 * Dependencies that will be used by any lagompb based project
 */
object BuildSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  val akkaVersion: String = Dependencies.Versions.AkkaVersion

  override def projectSettings =
    Seq(
      libraryDependencies ++= Dependencies.Jars ++ Seq(
        lagomScaladslApi,
        lagomScaladslServer,
        filters,
        lagomScaladslCluster,
        lagomScaladslPersistenceJdbc,
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit
      ) ++ Dependencies.TestJars,
      dependencyOverrides ++= Dependencies.AkkaOverrideDeps
    )
}
