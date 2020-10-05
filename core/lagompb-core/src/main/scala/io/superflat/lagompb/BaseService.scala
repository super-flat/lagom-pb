/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

/**
 * Must be implemented by any lagom api without message broker integration
 */
trait BaseService extends Service {

  protected val serviceName: String = ConfigReader.serviceName

  implicit def messageSerializer[A <: GeneratedMessage: GeneratedMessageCompanion]: ApiSerializer[A] = ApiSerializer[A]

  final override def descriptor: Descriptor = {
    import Service._

    routes
      .foldLeft(named(serviceName).withAutoAcl(true))(_.addCalls(_))

  }

  /**
   * routes define the various routes handled by the service.
   */
  def routes: Seq[Descriptor.Call[_, _]]
}
