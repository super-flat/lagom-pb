package lagompb

object CoverageWhitelist {

  val whitelist = Seq(
    "<empty>",
    "io.superflat.lagompb.protobuf.*",
    "io.superflat.lagompb.data.*",
    "io.superflat.lagompb.testkit.*",
    "io.superflat.lagompb.readside.*",
    "io.superflat.lagompb.LagompbBaseServiceImpl",
    "io.superflat.lagompb.LagompbGrpcServiceImpl",
    "io.superflat.lagompb.LagompbServiceImpl",
    "io.superflat.lagompb.InternalServerError"
  )
}
