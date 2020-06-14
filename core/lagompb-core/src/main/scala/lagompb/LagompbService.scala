package lagompb

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import lagompb.core.KafkaEvent

/**
 * Must be implemented by any lagom api without message broker integration
 */
trait LagompbService extends Service {

  protected val serviceName: String =
    LagompbConfig.serviceName

  final override def descriptor: Descriptor = {
    import Service._
    var namedService: Descriptor = named(serviceName)
      .withAutoAcl(true)

    // Let us set the route
    routes.foreach { calls =>
      namedService = namedService.addCalls(calls)
    }

    namedService
  }

  /** routes define the various routes handled by the service. */
  def routes: Seq[Descriptor.Call[_, _]]
}

/**
 * Must be implemented by any lagom api with kafka as message broker
 */
trait LagompbServiceWithKafka extends Service {

  protected val serviceName: String =
    LagompbConfig.serviceName

  final override def descriptor: Descriptor = {
    import Service._
    var namedService: Descriptor = named(serviceName)
    // set the kafka topics when necessary
      .withTopics(
        topic(s"$serviceName.events", kafkaEvents)(new LagompbKafkaSerde)
          .addProperty(KafkaProperties.partitionKeyStrategy, PartitionKeyStrategy[KafkaEvent](_.partitionKey))
      )
      .withAutoAcl(true)

    // Let us set the route
    routes.foreach { calls =>
      namedService = namedService.addCalls(calls)
    }

    namedService
  }

  /** routes define the various routes handled by the service. */
  def routes: Seq[Descriptor.Call[_, _]]

  /**
   * handle KafkaEvent topic
   *
   * @return the KafkaEvent topic handler
   */
  def kafkaEvents: Topic[KafkaEvent]
}
