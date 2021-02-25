import com.lightbend.lagom.core.LagomVersion
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
    val MacWireVersion = "2.3.7"
    val ScalaTestVersion = "3.2.4"
    val PlayJsonDerivedCodecsVersion = "7.0.0"
    val AkkaDiscoveryKubernetesApiVersion = "1.0.9"
    val PostgresDriverVersion = "42.2.19"
    val AkkaManagementVersion = "1.0.9"
    val AkkaManagementClusterBootstrapVersion = "1.0.9"
    val AkkaManagementClusterHttpVersion = "1.0.9"
    val ScalaMockVersion = "5.1.0"
    val SilencerVersion = "1.6.0"
    val AkkaGrpcVersion = "1.0.2"
    val H2Version = "1.4.200"
    val ScalapbJson4sVersion = "0.10.3"
    val PlayGrpcVersion = "0.9.1"
    val ReflectionsVersion = "0.9.12"
    val ScalapbCommonProtoVersion = "1.18.1-1"
    val EmbeddedPostgresVersion = "0.13.3"
    val EmbeddedKafkaVersion = "2.7.0"
    val AkkaProjectionVersion = "1.1.0"
    val CatsVersion = "2.4.2"

    val SbtProtocVersion = "1.0.0"
    val ScalapbCompilerVersion = "0.10.11"
    val ScalapbValidationVersion = "0.2.2"
    val JavaAgentVersion = "0.1.5"
    val CrossScalaForPlugin = Seq(Scala212)
    val AkkaVersion = "2.6.13"
  }

  object Compile {
    val Macwire: ModuleID = "com.softwaremill.macwire" %% "macros" % Versions.MacWireVersion

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

    val H2Driver: ModuleID = "com.h2database" % "h2" % Versions.H2Version
    val ScalapbJson4s: ModuleID = "com.thesamet.scalapb" %% "scalapb-json4s" % Versions.ScalapbJson4sVersion
    val Reflections: ModuleID = "org.reflections" % "reflections" % Versions.ReflectionsVersion

    val ScalapbCommonProtos: ModuleID =
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.ScalapbCommonProtoVersion
    val AkkaProjectionCore: ModuleID = "com.lightbend.akka" %% "akka-projection-core" % Versions.AkkaProjectionVersion
    val AkkaProjectionSlick: ModuleID = "com.lightbend.akka" %% "akka-projection-slick" % Versions.AkkaProjectionVersion
    val AkkaProjectionKafka: ModuleID = "com.lightbend.akka" %% "akka-projection-kafka" % Versions.AkkaProjectionVersion

    val AkkaProjectionEventSourced: ModuleID =
      "com.lightbend.akka" %% "akka-projection-eventsourced" % Versions.AkkaProjectionVersion

    val CatsCore = "org.typelevel" %% "cats-core" % Versions.CatsVersion
    val AkkaHttp = "com.typesafe.akka" %% "akka-http2-support" % LagomVersion.akkaHttp
    val AkkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.AkkaVersion
    val AkkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % Versions.AkkaVersion
    val AkkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % Versions.AkkaVersion
    val AkkaCluster = "com.typesafe.akka" %% "akka-cluster-typed" % Versions.AkkaVersion
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
    val AkkaMultiNodeTestkit: ModuleID = "com.typesafe.akka" %% "akka-multi-node-testkit" % Versions.AkkaVersion
    val AkkaTestkit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % Versions.AkkaVersion
    val AkkaStreamTestkit: ModuleID = "com.typesafe.akka" %% "akka-stream-testkit" % Versions.AkkaVersion
    val AkkaActorTestkitTyped: ModuleID = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.AkkaVersion
    val EmbeddedPostgres: ModuleID =
      "com.opentable.components" % "otj-pg-embedded" % Versions.EmbeddedPostgresVersion % "test"
    val EmbeddedKafka: ModuleID = "io.github.embeddedkafka" %% "embedded-kafka" % Versions.EmbeddedKafkaVersion % "test"
  }

  val SbtPlugin = Seq(
    "com.lightbend.play" %% "play-grpc-generators" % Dependencies.Versions.PlayGrpcVersion,
    "com.thesamet.scalapb" %% "compilerplugin" % Dependencies.Versions.ScalapbCompilerVersion,
    "com.thesamet.scalapb" %% "scalapb-validate-codegen" % Dependencies.Versions.ScalapbValidationVersion
  )

  val akkaVersion: String = Dependencies.Versions.AkkaVersion
  val AkkaOverrideDeps = Seq(
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
}
