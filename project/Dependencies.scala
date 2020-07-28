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
    val ScalaTestVersion = "3.2.0"
    val PlayJsonDerivedCodecsVersion = "7.0.0"
    val AkkaDiscoveryKubernetesApiVersion = "1.0.8"
    val PostgresDriverVersion = "42.2.14"
    val AkkaManagementVersion = "1.0.8"
    val AkkaManagementClusterBootstrapVersion = "1.0.8"
    val AkkaManagementClusterHttpVersion = "1.0.8"
    val JwtPlayJsonVersion = "4.3.0"
    val SlickMigrationApiVersion = "0.7.0"
    val ScalaMockVersion = "5.0.0"
    val KamonVersion = "2.1.4"
    val KanelaVersion = "1.0.5"
    val LogstashLogbackVersion = "6.3"
    val SilencerVersion = "1.6.0"
    val AkkaGrpcVersion = "0.8.4"
    val AkkaVersion: String = "2.6.6"
    val H2Version = "1.4.200"
    val JaninoVersion = "3.1.2"
    val ScalapbJson4sVersion = "0.10.1"
    val PlayGrpcVersion = "0.8.2"
    val ReflectionsVersion = "0.9.12"
    val ScalaClassFinderVersion = "1.5.1"
    val ApacheCommonValidatorVersion = "1.6"
    val GoogleRe2jVersion = "1.4"
    val GoogleProtobufUtilVersion = "3.12.2"
    val ScalapbCommonProtoVersion = "1.18.0-0"
    val EmbeddedPostgresVersion = "0.13.3"
    val EmbeddedKafkaVersion = "2.5.0"
    val AkkaProjectionVersion = "0.3"
    val CatsVersion = "2.1.1"

    val LagomVersion = "1.6.3"
    val SbtProtocVersion = "0.99.34"
    val ScalapbCompilerVersion = "0.10.7"
    val ScalapbValidationVersion = "0.1.2"
    val CrossScalaForPlugin = Seq(Scala212)
  }

  object Compile {
    val Macwire: ModuleID = "com.softwaremill.macwire" %% "macros" % Versions.MacwireVersion

    val PlayJsonDerivedCodecs: ModuleID =
      "org.julienrf" %% "play-json-derived-codecs" % Versions.PlayJsonDerivedCodecsVersion

    val LagomScaladslAkkaDiscovery: ModuleID =
      "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current

    val AkkaKubernetesDiscoveryApi: ModuleID =
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % Versions.AkkaDiscoveryKubernetesApiVersion
    val postgresDriver: ModuleID = "org.postgresql" % "postgresql" % Versions.PostgresDriverVersion

    val AkkaServiceLocator: ModuleID =
      "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current
    val akkaManagement: ModuleID = "com.lightbend.akka.management" %% "akka-management" % Versions.AkkaManagementVersion

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

    val LogstashLogback: ModuleID =
      "net.logstash.logback" % "logstash-logback-encoder" % Versions.LogstashLogbackVersion
    val H2Driver: ModuleID = "com.h2database" % "h2" % Versions.H2Version
    val Janino: ModuleID = "org.codehaus.janino" % "janino" % Versions.JaninoVersion
    val ScalapbJson4s: ModuleID = "com.thesamet.scalapb" %% "scalapb-json4s" % Versions.ScalapbJson4sVersion
    val Reflections: ModuleID = "org.reflections" % "reflections" % Versions.ReflectionsVersion
    val ScalaClassFinder: ModuleID = "org.clapper" %% "classutil" % Versions.ScalaClassFinderVersion

    val ApacheCommonValidator: ModuleID =
      "commons-validator" % "commons-validator" % Versions.ApacheCommonValidatorVersion
    val GoogleRe2j: ModuleID = "com.google.re2j" % "re2j" % Versions.GoogleRe2jVersion
    val GoogleProtobufUtil: ModuleID = "com.google.protobuf" % "protobuf-java-util" % Versions.GoogleProtobufUtilVersion

    val ScalapbCommonProtos: ModuleID =
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.ScalapbCommonProtoVersion
    val AkkaProjectionCore: ModuleID = "com.lightbend.akka" %% "akka-projection-core" % Versions.AkkaProjectionVersion
    val AkkaProjectionSlick: ModuleID = "com.lightbend.akka" %% "akka-projection-slick" % Versions.AkkaProjectionVersion
    val AkkaProjectionKafka: ModuleID = "com.lightbend.akka" %% "akka-projection-kafka" % Versions.AkkaProjectionVersion

    val AkkaProjectionEventSourced: ModuleID =
      "com.lightbend.akka" %% "akka-projection-eventsourced" % Versions.AkkaProjectionVersion

    val CatsCore = "org.typelevel" %% "cats-core" % Versions.CatsVersion
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
    Dependencies.Compile.PlayJsonDerivedCodecs,
    Dependencies.Compile.LagomScaladslAkkaDiscovery,
    Dependencies.Compile.postgresDriver,
    Dependencies.Compile.Macwire,
    Dependencies.Compile.AkkaServiceLocator,
    Dependencies.Compile.akkaManagement,
    Dependencies.Compile.AkkaManagementClusterBootstrap,
    Dependencies.Compile.AkkaManagementClusterHttp,
    Dependencies.Compile.AkkaKubernetesDiscoveryApi,
    Dependencies.Compile.JwtPlayJson,
    Dependencies.Compile.ScalapbJson4s,
    Dependencies.Compile.Janino,
    Dependencies.Compile.Reflections,
    Dependencies.Compile.KamonBundle,
    Dependencies.Compile.KamonPrometheus,
    Dependencies.Compile.KamonJaeger,
    Dependencies.Compile.LogstashLogback,
    Dependencies.Compile.ScalaClassFinder,
    Dependencies.Compile.ApacheCommonValidator,
    Dependencies.Compile.GoogleRe2j,
    Dependencies.Compile.GoogleProtobufUtil,
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
