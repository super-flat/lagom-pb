package lagompb

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.EmptyJsonSerializerRegistry
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
import com.lightbend.lagom.scaladsl.server.LagomServer
import kamon.Kamon
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.filters.hosts.AllowedHostsComponents

abstract class LagompbApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with JdbcPersistenceComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with LagomKafkaComponents
    with AhcWSComponents
    with CORSComponents
    with AllowedHostsComponents
    with CSRFComponents
    with SecurityHeadersComponents {
  // lagomServer is set by the server definition in the implementation class
  override lazy val lagomServer: LagomServer = server

  // Json Serializer registry not needed
  final override lazy val jsonSerializerRegistry: JsonSerializerRegistry = EmptyJsonSerializerRegistry

  // set the security filters
  override val httpFilters: Seq[EssentialFilter] =
    Seq(corsFilter, allowedHostsFilter, csrfFilter, securityHeadersFilter)

  /**
   * Defines the persistent entity that will be used to handle commands
   *
   * @see [[lagompb.LagompbAggregate]].
   *      Also for more info refer to the lagom doc [[https://www.lagomframework.com/documentation/1.6.x/scala/UsingAkkaPersistenceTyped.html]]
   */
  def aggregateRoot: LagompbAggregate[_]

  /** server helps define the lagom server. Please refer to the lagom doc
   * @example
   *          override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  def server: LagomServer

  /** serviceLocator is used to enable service discovery and api gateway
   * @see [[https://www.lagomframework.com/documentation/1.6.x/scala/ServiceLocator.html]]
   * @return ServiceLocator
   */
  override def serviceLocator: ServiceLocator

  // initialize cluster sharding
  clusterSharding.init(
    Entity(aggregateRoot.typeKey)(
      entityContext => aggregateRoot.create(entityContext)
    )
  )
}
