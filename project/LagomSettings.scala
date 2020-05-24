package lagompb

import com.lightbend.lagom.sbt.LagomImport.lagomScaladslApi
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslCluster
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslKafkaBroker
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslPersistenceJdbc
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslServer
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslTestKit
import com.lightbend.lagom.sbt.LagomImport.lagomScaladslPersistenceCassandra
import lagompb.Dependencies.Compile
import lagompb.Dependencies.Runtime
import play.sbt.PlayImport.filters
import sbt.AutoPlugin
import sbt.Plugins
import sbt.plugins
import sbt.Keys.libraryDependencies

/**
 * Dependencies that will be used by any lagompb based project
 */
object LagomSettings extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings = Seq(
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
      Compile.commonProtos,
      Runtime.akkaGrpcRuntime,
      Runtime.scalapbRuntime,
      Runtime.playGrpcRuntime,
      Runtime.scalaReflect,
      Runtime.commonProtosRuntime,
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
