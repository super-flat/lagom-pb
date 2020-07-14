import Dependencies.Compile
import Dependencies.Runtime
import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt.Keys.libraryDependencies
import sbt.{plugins, AutoPlugin, Plugins}

/**
 * Dependencies that will be used by any lagompb based project
 */
object LagomSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings =
    Seq(
      libraryDependencies ++= Seq(
        lagomScaladslApi,
        lagomScaladslServer,
        filters,
        lagomScaladslCluster,
        lagomScaladslPersistenceJdbc,
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Compile.playJsonDerivedCodecs,
        Compile.lagomScaladslAkkaDiscovery,
        Compile.postgresDriver,
        Compile.macwire,
        Compile.akkaServiceLocator,
        Compile.akkaManagement,
        Compile.akkaManagementClusterBootstrap,
        Compile.akkaManagementClusterHttp,
        Compile.akkaKubernetesDiscoveryApi,
        Compile.jwtPlayJson,
        Compile.scalapbJson4s,
        Compile.janino,
        Compile.reflections,
        Compile.kamonBundle,
        Compile.kamonPrometheus,
        Compile.kamonJaeger,
        Compile.logstashLogback,
        Compile.scalaClassFinder,
        Compile.apacheCommonValidator,
        Compile.googleRe2j,
        Compile.googleProtobufUtil,
        Compile.scalapbCommonProtos,
        Compile.akkaProjectionCore,
        Compile.akkaProjectionKafka,
        Compile.akkaProjectionSlick,
        Compile.akkaProjectionEventSourced,
        Runtime.akkaGrpcRuntime,
        Runtime.scalapbRuntime,
        Runtime.scalapbValidationRuntime,
        Runtime.playGrpcRuntime,
        Runtime.scalaReflect,
        Runtime.scalapbCommonProtosRuntime,
        Dependencies.Test.scalaTest,
        Dependencies.Test.scalaMock,
        Dependencies.Test.akkaMultiNodeTeskit,
        Dependencies.Test.akkaTestkit,
        Dependencies.Test.akkaStreamTestkit,
        Dependencies.Test.akkaActorTeskitTyped,
        Dependencies.Test.embeddedPostgres,
        Dependencies.Test.embeddedKafka
      )
    )
}
