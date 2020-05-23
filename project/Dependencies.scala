package lagompb

import com.lightbend.lagom.core.LagomVersion
import scalapb.compiler.Version.scalapbVersion
import sbt._

/**
 * Holds the list of dependencies used in the project and their various version
 * It gives a central place to quickly update dependencies
 */
object Dependencies {

  /**
   * Versions number
   */
  object Versions {
    val scala213 = "2.13.1"
    val macwireVersion = "2.3.3"
    val scalaTestVersion = "3.1.2"
    val playJsonDerivedCodecsVersion = "7.0.0"
    val akkaDiscoveryKubernetesApiVersion = "1.0.7"
    val postgresDriverVersion = "42.2.12"
    val akkaManagementVersion = "1.0.7"
    val akkaManagementClusterBootstrapVersion = "1.0.7"
    val akkaManagementClusterHttpVersion = "1.0.7"
    val jwtPlayJsonVersion = "4.3.0"
    val slickMigrationApiVersion = "0.7.0"
    val scalaMockVersion = "4.4.0"
    val kamonVersion = "2.1.0"
    val kanelaVersion = "1.0.5"
    val logstashLogbackVersion = "6.3"
    val silencerVersion = "1.6.0"
    val akkaGrpcRuntimeVersion = "0.8.4"
    val akkaVersion: String = LagomVersion.akka
    val h2Version = "1.4.200"
    val janinoVersion = "3.1.2"
    val scalapbJson4sVersion = "0.10.1"
    val playGrpcRuntimeVersion = "0.8.2"
    val reflectionsVersion = "0.9.12"
    val scalaClassFinderVersion = "1.5.1"
    val apacheCommonValidatorVersion = "1.6"
    val googleRe2jVersion = "1.3"
    val googleProtobufUtilVersion = "3.11.4"
    val commonProtoVersion = "1.17.0-0"
    val embeddedPostgresVersion = "0.13.3"
    val embeddedKafkaVersion = "2.5.0"
  }

  object Compile {
    val macwire: ModuleID = "com.softwaremill.macwire" %% "macros" % Versions.macwireVersion
    val playJsonDerivedCodecs
        : ModuleID = "org.julienrf" %% "play-json-derived-codecs" % Versions.playJsonDerivedCodecsVersion
    val lagomScaladslAkkaDiscovery
        : ModuleID = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current
    val akkaKubernetesDiscoveryApi
        : ModuleID = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % Versions.akkaDiscoveryKubernetesApiVersion
    val postgresDriver: ModuleID = "org.postgresql" % "postgresql" % Versions.postgresDriverVersion
    val akkaServiceLocator
        : ModuleID = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current
    val akkaManagement: ModuleID = "com.lightbend.akka.management" %% "akka-management" % Versions.akkaManagementVersion
    val akkaManagementClusterBootstrap
        : ModuleID = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Versions.akkaManagementClusterBootstrapVersion
    val akkaManagementClusterHttp
        : ModuleID = "com.lightbend.akka.management" %% "akka-management-cluster-http" % Versions.akkaManagementClusterHttpVersion
    val jwtPlayJson: ModuleID = "com.pauldijou" %% "jwt-play-json" % Versions.jwtPlayJsonVersion
    val slickMigrationApi: ModuleID = "io.github.nafg" %% "slick-migration-api" % Versions.slickMigrationApiVersion
    val kamonBundle: ModuleID = "io.kamon" %% "kamon-bundle" % Versions.kamonVersion
    val kamonPrometheus: ModuleID = "io.kamon" %% "kamon-prometheus" % Versions.kamonVersion
    val kamonZipkin: ModuleID = "io.kamon" %% "kamon-zipkin" % Versions.kamonVersion
    val kamonJaeger: ModuleID = "io.kamon" %% "kamon-jaeger" % Versions.kamonVersion
    val kanela: ModuleID = "io.kamon" % "kanela-agent" % Versions.kanelaVersion
    val logstashLogback
        : ModuleID = "net.logstash.logback" % "logstash-logback-encoder" % Versions.logstashLogbackVersion
    val h2Driver: ModuleID = "com.h2database" % "h2" % Versions.h2Version
    val janino: ModuleID = "org.codehaus.janino" % "janino" % Versions.janinoVersion
    val scalapbJson4s: ModuleID = "com.thesamet.scalapb" %% "scalapb-json4s" % Versions.scalapbJson4sVersion
    val reflections: ModuleID = "org.reflections" % "reflections" % Versions.reflectionsVersion
    val scalaClassFinder: ModuleID = "org.clapper" %% "classutil" % Versions.scalaClassFinderVersion
    val apacheCommonValidator
        : ModuleID = "commons-validator" % "commons-validator" % Versions.apacheCommonValidatorVersion
    val googleRe2j: ModuleID = "com.google.re2j" % "re2j" % Versions.googleRe2jVersion
    val googleProtobufUtil: ModuleID = "com.google.protobuf" % "protobuf-java-util" % Versions.googleProtobufUtilVersion
    val commonProtos
        : ModuleID = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.commonProtoVersion
  }

  object Runtime {
    val scalapbRuntime: ModuleID = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf"
    val akkaGrpcRuntime: ModuleID = "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % Versions.akkaGrpcRuntimeVersion
    val playGrpcRuntime: ModuleID = "com.lightbend.play" %% "play-grpc-runtime" % Versions.playGrpcRuntimeVersion
    val scalaReflect: ModuleID = "org.scala-lang" % "scala-reflect" % Versions.scala213
    val commonProtosRuntime
        : ModuleID = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % Versions.commonProtoVersion % "protobuf"
  }

  object Test {
    val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % Versions.scalaTestVersion
    val scalaMock: ModuleID = "org.scalamock" %% "scalamock" % Versions.scalaMockVersion
    val akkaMultiNodeTeskit: ModuleID = "com.typesafe.akka" %% "akka-multi-node-testkit" % Versions.akkaVersion
    val akkaTestkit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % Versions.akkaVersion
    val akkaStreamTestkit: ModuleID = "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akkaVersion
    val akkaActorTeskitTyped: ModuleID = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akkaVersion
    val embeddedPostgres: ModuleID = "com.opentable.components" % "otj-pg-embedded" % Versions.embeddedPostgresVersion
    val embeddedKafka: ModuleID = "io.github.embeddedkafka" %% "embedded-kafka" % Versions.embeddedKafkaVersion
  }

}
