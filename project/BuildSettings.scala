import Dependencies.{Compile, Runtime}
import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt.Keys.{dependencyOverrides, libraryDependencies}
import sbt.{AutoPlugin, Plugins, plugins, _}

/**
 * Dependencies that will be used by any lagompb based project
 */
object BuildSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  val akkaVersion: String = Dependencies.Versions.AkkaVersion

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
        Compile.LagomAkkaServiceLocator,
        Compile.AkkaHttp,
        Compile.AkkaStream,
        Compile.AkkaManagement,
        Compile.AkkaDiscovery,
        Compile.AkkaCluster,
        Compile.AkkaClusterSharding,
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
      ),
      dependencyOverrides ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-remote" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-coordination" % akkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
        "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
        "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion,
        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
      )
    )
}
