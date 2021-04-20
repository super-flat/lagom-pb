import com.lightbend.lagom.core.LagomVersion
import sbt.{ Test, _ }
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
    val Scala213 = "2.13.5"
    val Scala212 = "2.12.13"
    val MacWireVersion = "2.3.7"
    val ScalaTestVersion = "3.2.8"
    val PlayJsonDerivedCodecsVersion = "7.0.0"
    val AkkaDiscoveryKubernetesApiVersion = "1.0.10"
    val PostgresDriverVersion = "42.2.19"
    val AkkaManagementVersion = "1.0.10"
    val AkkaManagementClusterBootstrapVersion = "1.0.10"
    val AkkaManagementClusterHttpVersion = "1.0.10"
    val ScalaMockVersion = "5.1.0"
    val SilencerVersion = "1.7.3"
    val AkkaGrpcVersion = "1.0.3"
    val H2Version = "1.4.200"
    val ScalapbJson4sVersion = "0.11.0"
    val PlayGrpcVersion = "0.9.1"
    val ReflectionsVersion = "0.9.12"
    val ScalapbCommonProtoVersion = "1.18.1-1"
    val AkkaProjectionVersion = "1.1.0"
    val CatsVersion = "2.5.0"

    val SbtProtocVersion = "1.0.2"
    val ScalapbCompilerVersion = scalapbVersion
    val ScalapbValidationVersion = scalapb.validate.compiler.BuildInfo.version
    val JavaAgentVersion = "0.1.6"
    val CrossScalaForPlugin = Seq(Scala212)
    val AkkaVersion = "2.6.14"

    val TestContainers: String = "0.39.3"

    // This is not the sbt version used by lagom-pb build itself, but
    // instead the version used to build lagom-pb sbt plugin.
    val TargetSbt1 = "1.3.13"
  }

  val Jars = Seq(
    "com.softwaremill.macwire" %% "macros" % Versions.MacWireVersion,
    "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current,
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % Versions.AkkaDiscoveryKubernetesApiVersion,
    "org.postgresql" % "postgresql" % Versions.PostgresDriverVersion,
    "org.typelevel" %% "cats-core" % Versions.CatsVersion,
    "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current,
    "com.lightbend.akka.management" %% "akka-management" % Versions.AkkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Versions.AkkaManagementClusterBootstrapVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % Versions.AkkaManagementClusterHttpVersion,
    "com.h2database" % "h2" % Versions.H2Version,
    "com.thesamet.scalapb" %% "scalapb-json4s" % Versions.ScalapbJson4sVersion,
    "org.reflections" % "reflections" % Versions.ReflectionsVersion,
    // Akka Projection
    "com.lightbend.akka" %% "akka-projection-core" % Versions.AkkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-slick" % Versions.AkkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-kafka" % Versions.AkkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-eventsourced" % Versions.AkkaProjectionVersion,
    // Akka clustering
    "com.typesafe.akka" %% "akka-http2-support" % LagomVersion.akkaHttp,
    "com.typesafe.akka" %% "akka-stream" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % Versions.AkkaVersion,
    // Runtime
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version,
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.ScalapbCommonProtoVersion % "protobuf",
    "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % Versions.AkkaGrpcVersion,
    "com.lightbend.play" %% "play-grpc-runtime" % Versions.PlayGrpcVersion,
    "org.scala-lang" % "scala-reflect" % Versions.Scala213)

  /**
   * Test dependencies
   */
  val TestJars: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.ScalaTestVersion,
    "org.scalamock" %% "scalamock" % Versions.ScalaMockVersion,
    "com.typesafe.akka" %% "akka-multi-node-testkit" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.AkkaVersion,
    // test containers
    "com.dimafeng" %% "testcontainers-scala-scalatest" % Versions.TestContainers % Test,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.TestContainers % Test)

  val SbtPlugin = Seq(
    "com.lightbend.play" %% "play-grpc-generators" % Dependencies.Versions.PlayGrpcVersion,
    "com.thesamet.scalapb" %% "compilerplugin" % Dependencies.Versions.ScalapbCompilerVersion,
    "com.thesamet.scalapb" %% "scalapb-validate-codegen" % Dependencies.Versions.ScalapbValidationVersion)

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
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion)
}
