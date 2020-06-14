package lagompb

import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

final case class SnapshotCriteria(frequency: Int, retention: Int)

final case class EventsConfig(tagName: String, numShards: Int)

object LagompbConfig {
  private lazy val config: Config = ConfigFactory.load()
  private val LP = "lagompb"

  def serviceName: String = config.getString(s"$LP.service-name")

  def protosPackage: String = config.getString(s"$LP.protos-package")

  def askTimeout: Timeout = Timeout(config.getInt("lagompb.ask-timeout").seconds)

  def snapshotCriteria: SnapshotCriteria = {
    SnapshotCriteria(
      frequency = config.getInt(s"$LP.snapshot-criteria.frequency"),
      retention = config.getInt(s"$LP.snapshot-criteria.retention")
    )
  }

  def eventsConfig: EventsConfig = {
    EventsConfig(
      tagName = config.getString(s"$LP.events.tagname"),
      numShards = config.getInt("akka.cluster.sharding.number-of-shards")
    )
  }

  def allEventTags: Vector[String] = {
    (for (shardNo <- 0 until LagompbConfig.eventsConfig.numShards)
      yield s"${LagompbConfig.eventsConfig.tagName}$shardNo").toVector
  }
}
