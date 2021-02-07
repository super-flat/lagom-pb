/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import akka.grpc.sbt.AkkaGrpcPlugin
import com.lightbend.lagom.sbt.LagomPlugin
import sbt.AutoPlugin
import sbtprotoc.ProtocPlugin

object Plugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = LagomPlugin && ProtocPlugin && AkkaGrpcPlugin
}
