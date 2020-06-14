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
