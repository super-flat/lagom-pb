package io.superflat.lagompb

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{EmptyJsonSerializerRegistry, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import io.superflat.lagompb.encryption.{EncryptionAdapter, ProtoEncryption}
import kamon.Kamon
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.filters.hosts.AllowedHostsComponents

sealed trait BaseApplicationComponents
    extends AhcWSComponents
    with CORSComponents
    with AllowedHostsComponents
    with CSRFComponents
    with SecurityHeadersComponents

sealed trait PostgresPersistenceComponents
    extends JdbcPersistenceComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with LagomKafkaComponents

/**
 * LagompbApplication an abstract class that will be implemented to define the lagom application that needs
 * akka persistence for journal and snapshot
 *
 * @param context the lagom application context
 */
abstract class BaseApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with PostgresPersistenceComponents
    with BaseApplicationComponents {

  // $COVERAGE-OFF$
  // initialize instrumentation and tracing if it is enabled
  Kamon.init()

  loadProtosRegistry()

  // Json Serializer registry not needed
  final override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    EmptyJsonSerializerRegistry

  // lagomServer is set by the server definition in the implementation class
  override lazy val lagomServer: LagomServer = server

  // set the security filters
  override val httpFilters: Seq[EssentialFilter] =
    Seq(corsFilter, allowedHostsFilter, csrfFilter, securityHeadersFilter)

  // define an encryptor (default to None)
  def protoEncryption: Option[ProtoEncryption] = None

  // create an encryption adapter with above ProtoEncryption
  final lazy val encryptionAdapter: EncryptionAdapter = new EncryptionAdapter(protoEncryption)

  /**
   * Defines the persistent entity that will be used to handle commands
   *
   * @see [[io.superflat.lagompb.AggregateRoot]].
   *      Also for more info refer to the lagom doc [[https://www.lagomframework.com/documentation/1.6.x/scala/UsingAkkaPersistenceTyped.html]]
   */
  def aggregateRoot: AggregateRoot[_]

  /**
   * server helps define the lagom server. Please refer to the lagom doc
   * @example
   *          override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  def server: LagomServer

  /**
   * serviceLocator is used to enable service discovery and api gateway
   * @see [[https://www.lagomframework.com/documentation/1.6.x/scala/ServiceLocator.html]]
   * @return ServiceLocator
   */
  override def serviceLocator: ServiceLocator

  def selectShard(numShards: Int, entityId: String): Int =
    Math.abs(entityId.hashCode) % numShards

  // initialize cluster sharding
  clusterSharding.init(Entity(aggregateRoot.typeKey) { entityContext =>
    val shardIndex =
      selectShard(ConfigReader.eventsConfig.numShards, entityContext.entityId)
    aggregateRoot.create(entityContext, shardIndex)
  })

  def loadProtosRegistry(): Unit = {
    ProtosRegistry.registry
    ProtosRegistry.typeRegistry
  }

  // $COVERAGE-ON$
}

/**
 * LagompbStatelessApplication an abstract class that will be implemented to define the lagom application that does not need
 * akka persistence. Therefore no events and state are persisted
 *
 * @param context the lagom application context
 */
abstract class BaseStatelessApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with BaseApplicationComponents {

  // $COVERAGE-OFF$

  // lagomServer is set by the server definition in the implementation class
  override lazy val lagomServer: LagomServer = server

  /**
   * server helps define the lagom server. Please refer to the lagom doc
   * @example
   *          override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  def server: LagomServer

  /**
   * serviceLocator is used to enable service discovery and api gateway
   * @see [[https://www.lagomframework.com/documentation/1.6.x/scala/ServiceLocator.html]]
   * @return ServiceLocator
   */
  override def serviceLocator: ServiceLocator

  // $COVERAGE-ON$
}
