package io.superflat.lagompb

import akka.grpc.sbt.AkkaGrpcPlugin
import com.lightbend.lagom.sbt.LagomPlugin
import sbt.AutoPlugin
import sbtprotoc.ProtocPlugin

object LagompbPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = LagomPlugin && ProtocPlugin && AkkaGrpcPlugin
}
