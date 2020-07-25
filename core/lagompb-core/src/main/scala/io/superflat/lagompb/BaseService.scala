package io.superflat.lagompb

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

/**
  * Must be implemented by any lagom api without message broker integration
  */
trait BaseService extends Service {

  protected val serviceName: String =
    ConfigReader.serviceName

  implicit def messageSerializer[
      A <: GeneratedMessage: GeneratedMessageCompanion
  ]: ApiSerializer[A] =
    new ApiSerializer[A]

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
