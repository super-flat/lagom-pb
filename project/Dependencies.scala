import com.lightbend.lagom.core.LagomVersion
import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt._
import scalapb.compiler.Version.scalapbVersion

/**
 * Holds the list of dependencies used in the project and their various version
 * It gives a central place to quickly update dependencies
 */
object Dependencies {

  /**
   * Versions number
   */
  object Versions {
    val Scala213 = "2.13.1"
    val Scala212 = "2.12.11"
    val MacwireVersion = "2.3.7"
    val ScalaTestVersion = "3.2.2"
    val PlayJsonDerivedCodecsVersion = "7.0.0"
    val AkkaDiscoveryKubernetesApiVersion = "1.0.8"
    val PostgresDriverVersion = "42.2.16"
    val AkkaManagementVersion = "1.0.8"
    val AkkaManagementClusterBootstrapVersion = "1.0.8"
    val AkkaManagementClusterHttpVersion = "1.0.8"
    val JwtPlayJsonVersion = "4.3.0"
    val SlickMigrationApiVersion = "0.7.0"
    val ScalaMockVersion = "5.0.0"
    val KamonVersion = "2.1.6"
    val KanelaVersion = "1.0.6"
    val SilencerVersion = "1.6.0"
    val AkkaGrpcVersion = "1.0.0"
    val H2Version = "1.4.200"
    val ScalapbJson4sVersion = "0.10.1"
    val PlayGrpcVersion = "0.9.0"
    val ReflectionsVersion = "0.9.12"
    val ApacheCommonValidatorVersion = "1.7"
    val ScalapbCommonProtoVersion = "1.18.0-0"
    val EmbeddedPostgresVersion = "0.13.3"
    val EmbeddedKafkaVersion = "2.6.0"
    val AkkaProjectionVersion = "1.0.0-RC3"
    val CatsVersion = "2.2.0"

    val LagomVersion = "1.6.4"
    val SbtProtocVersion = "0.99.34"
    val ScalapbCompilerVersion = "0.10.8"
    val ScalapbValidationVersion = "0.1.2"
    val JavaAgentVersion = "0.1.5"
    val CrossScalaForPlugin = Seq(Scala212)
  }

  object Compile {
    val Macwire: ModuleID = "com.softwaremill.macwire" %% "macros" % Versions.MacwireVersion

    val LagomScaladslAkkaDiscovery: ModuleID =
      "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current

    val AkkaKubernetesDiscoveryApi: ModuleID =
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % Versions.AkkaDiscoveryKubernetesApiVersion
    val postgresDriver: ModuleID = "org.postgresql" % "postgresql" % Versions.PostgresDriverVersion

    val LagomAkkaServiceLocator: ModuleID =
      "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current
    val AkkaManagement: ModuleID = "com.lightbend.akka.management" %% "akka-management" % Versions.AkkaManagementVersion

    val AkkaManagementClusterBootstrap: ModuleID =
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Versions.AkkaManagementClusterBootstrapVersion

    val AkkaManagementClusterHttp: ModuleID =
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % Versions.AkkaManagementClusterHttpVersion
    val JwtPlayJson: ModuleID = "com.pauldijou" %% "jwt-play-json" % Versions.JwtPlayJsonVersion
    val SlickMigrationApi: ModuleID = "io.github.nafg" %% "slick-migration-api" % Versions.SlickMigrationApiVersion
    val KamonBundle: ModuleID = "io.kamon" %% "kamon-bundle" % Versions.KamonVersion
    val KamonPrometheus: ModuleID = "io.kamon" %% "kamon-prometheus" % Versions.KamonVersion
    val KamonZipkin: ModuleID = "io.kamon" %% "kamon-zipkin" % Versions.KamonVersion
    val KamonJaeger: ModuleID = "io.kamon" %% "kamon-jaeger" % Versions.KamonVersion
    val Kanela: ModuleID = "io.kamon" % "kanela-agent" % Versions.KanelaVersion

    val H2Driver: ModuleID = "com.h2database" % "h2" % Versions.H2Version
    val ScalapbJson4s: ModuleID = "com.thesamet.scalapb" %% "scalapb-json4s" % Versions.ScalapbJson4sVersion
    val Reflections: ModuleID = "org.reflections" % "reflections" % Versions.ReflectionsVersion
    val ApacheCommonValidator: ModuleID =
      "commons-validator" % "commons-validator" % Versions.ApacheCommonValidatorVersion

    val ScalapbCommonProtos: ModuleID =
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.ScalapbCommonProtoVersion
    val AkkaProjectionCore: ModuleID = "com.lightbend.akka" %% "akka-projection-core" % Versions.AkkaProjectionVersion
    val AkkaProjectionSlick: ModuleID = "com.lightbend.akka" %% "akka-projection-slick" % Versions.AkkaProjectionVersion
    val AkkaProjectionKafka: ModuleID = "com.lightbend.akka" %% "akka-projection-kafka" % Versions.AkkaProjectionVersion

    val AkkaProjectionEventSourced: ModuleID =
      "com.lightbend.akka" %% "akka-projection-eventsourced" % Versions.AkkaProjectionVersion

    val CatsCore = "org.typelevel" %% "cats-core" % Versions.CatsVersion
    val AkkaHttp = "com.typesafe.akka" %% "akka-http2-support" % LagomVersion.akkaHttp
    val AkkaStream = "com.typesafe.akka" %% "akka-stream" % LagomVersion.akka
    val AkkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % LagomVersion.akka
    val AkkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % LagomVersion.akka
    val AkkaCluster = "com.typesafe.akka" %% "akka-cluster-typed" % LagomVersion.akka
  }

  object Runtime {
    val ScalapbRuntime: ModuleID = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion

    val ScalapbValidationRuntime =
      "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version
    val AkkaGrpcRuntime: ModuleID = "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % Versions.AkkaGrpcVersion
    val PlayGrpcRuntime: ModuleID = "com.lightbend.play" %% "play-grpc-runtime" % Versions.PlayGrpcVersion
    val ScalaReflect: ModuleID = "org.scala-lang" % "scala-reflect" % Versions.Scala213

    val ScalapbCommonProtosRuntime: ModuleID =
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.ScalapbCommonProtoVersion % "protobuf"
  }

  /**
   * Test dependencies
   */
  object Test {
    val ScalaTest: ModuleID = "org.scalatest" %% "scalatest" % Versions.ScalaTestVersion
    val ScalaMock: ModuleID = "org.scalamock" %% "scalamock" % Versions.ScalaMockVersion
    val AkkaMultiNodeTestkit: ModuleID = "com.typesafe.akka" %% "akka-multi-node-testkit" % LagomVersion.akka
    val AkkaTestkit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % LagomVersion.akka
    val AkkaStreamTestkit: ModuleID = "com.typesafe.akka" %% "akka-stream-testkit" % LagomVersion.akka
    val AkkaActorTestkitTyped: ModuleID = "com.typesafe.akka" %% "akka-actor-testkit-typed" % LagomVersion.akka
    val EmbeddedPostgres: ModuleID = "com.opentable.components" % "otj-pg-embedded" % Versions.EmbeddedPostgresVersion
    val EmbeddedKafka: ModuleID = "io.github.embeddedkafka" %% "embedded-kafka" % Versions.EmbeddedKafkaVersion
  }

  val SbtPlugin = Seq(
    "com.lightbend.play" %% "play-grpc-generators" % Dependencies.Versions.PlayGrpcVersion,
    "com.thesamet.scalapb" %% "compilerplugin" % Dependencies.Versions.ScalapbCompilerVersion,
    "com.thesamet.scalapb" %% "scalapb-validate-codegen" % Dependencies.Versions.ScalapbValidationVersion,
    lagomScaladslApi,
    lagomScaladslServer,
    filters,
    lagomScaladslCluster,
    lagomScaladslPersistenceJdbc,
    lagomScaladslPersistenceCassandra,
    lagomScaladslKafkaBroker,
    lagomScaladslTestKit,
    Dependencies.Compile.LagomScaladslAkkaDiscovery,
    Dependencies.Compile.postgresDriver,
    Dependencies.Compile.Macwire,
    Dependencies.Compile.LagomAkkaServiceLocator,
    Dependencies.Compile.AkkaManagement,
    Dependencies.Compile.AkkaManagementClusterBootstrap,
    Dependencies.Compile.AkkaManagementClusterHttp,
    Dependencies.Compile.AkkaKubernetesDiscoveryApi,
    Dependencies.Compile.JwtPlayJson,
    Dependencies.Compile.ScalapbJson4s,
    Dependencies.Compile.Reflections,
    Dependencies.Compile.KamonBundle,
    Dependencies.Compile.KamonPrometheus,
    Dependencies.Compile.KamonJaeger,
    Dependencies.Compile.ApacheCommonValidator,
    Dependencies.Compile.ScalapbCommonProtos,
    Dependencies.Compile.AkkaProjectionCore,
    Dependencies.Compile.AkkaProjectionKafka,
    Dependencies.Compile.AkkaProjectionSlick,
    Dependencies.Compile.AkkaProjectionEventSourced,
    Dependencies.Compile.CatsCore,
    Dependencies.Runtime.AkkaGrpcRuntime,
    Dependencies.Runtime.ScalapbRuntime,
    Dependencies.Runtime.ScalapbValidationRuntime,
    Dependencies.Runtime.PlayGrpcRuntime,
    Dependencies.Runtime.ScalapbCommonProtosRuntime,
    Dependencies.Test.ScalaTest,
    Dependencies.Test.ScalaMock,
    Dependencies.Test.AkkaMultiNodeTestkit,
    Dependencies.Test.AkkaTestkit,
    Dependencies.Test.AkkaStreamTestkit,
    Dependencies.Test.AkkaActorTestkitTyped,
    Dependencies.Test.EmbeddedPostgres,
    Dependencies.Test.EmbeddedKafka
  )
}
