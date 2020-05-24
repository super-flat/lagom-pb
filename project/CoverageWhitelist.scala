package lagompb

object CoverageWhitelist {
  val whitelist = Seq(
    "<empty>",
    "lagompb.protobuf.*",
    "lagompb.data.*",
    "lagompb.testkit.*",
    "lagompb.readside.*",
    "lagompb.LagompbEvent",
    "lagompb.LagompbServiceImplComponent",
    "lagompb.LagompbRestServiceImpl",
    "lagompb.LagompbGrpcServiceImpl",
    "lagompb.LagompbServiceImpl",
    "lagompb.LagompbServiceImplWithKafka",
    "lagompb.LagompbServiceWithKafka",
    "lagompb.InternalServerError"
  )
}
