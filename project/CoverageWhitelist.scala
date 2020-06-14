package lagompb

object CoverageWhitelist {

  val whitelist = Seq(
    "<empty>",
    "lagompb.core.*",
    "lagompb.extensions.*",
    "lagompb.options.*",
    "lagompb.tests.*",
    "lagompb.data.*",
    "lagompb.testkit.*",
    "lagompb.readside.*",
    "lagompb.LagompbEvent",
    "lagompb.LagompbBaseServiceImpl",
    "lagompb.LagompbRestServiceImpl",
    "lagompb.LagompbGrpcServiceImpl",
    "lagompb.LagompbServiceImpl",
    "lagompb.LagompbServiceImplWithKafka",
    "lagompb.LagompbServiceWithKafka",
    "lagompb.InternalServerError"
  )
}
