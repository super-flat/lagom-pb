import Dependencies.{Compile, Runtime}
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
        Compile.LagomScaladslAkkaDiscovery,
        Compile.postgresDriver,
        Compile.Macwire,
        Compile.AkkaServiceLocator,
        Compile.akkaManagement,
        Compile.AkkaManagementClusterBootstrap,
        Compile.AkkaManagementClusterHttp,
        Compile.AkkaKubernetesDiscoveryApi,
        Compile.JwtPlayJson,
        Compile.ScalapbJson4s,
        Compile.Reflections,
        Compile.KamonBundle,
        Compile.KamonPrometheus,
        Compile.KamonJaeger,
        Compile.ApacheCommonValidator,
        Compile.ScalapbCommonProtos,
        Compile.AkkaProjectionCore,
        Compile.AkkaProjectionKafka,
        Compile.AkkaProjectionSlick,
        Compile.AkkaProjectionEventSourced,
        Compile.CatsCore,
        Compile.AkkaHttp,
        Compile.AkkaStream,
        Runtime.AkkaGrpcRuntime,
        Runtime.ScalapbRuntime,
        Runtime.ScalapbValidationRuntime,
        Runtime.PlayGrpcRuntime,
        Runtime.ScalaReflect,
        Runtime.ScalapbCommonProtosRuntime,
        Dependencies.Test.ScalaTest,
        Dependencies.Test.ScalaMock,
        Dependencies.Test.AkkaMultiNodeTestkit,
        Dependencies.Test.AkkaTestkit,
        Dependencies.Test.AkkaStreamTestkit,
        Dependencies.Test.AkkaActorTestkitTyped,
        Dependencies.Test.EmbeddedPostgres,
        Dependencies.Test.EmbeddedKafka
      )
    )
}
