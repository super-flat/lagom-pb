object CoverageExclusionList {

  val whitelist = Seq(
    "<empty>",
    "io.superflat.lagompb.protobuf.*", // ignore scalapb generated classes
    "io.superflat.lagompb.data.*",
    "io.superflat.lagompb.testkit.*",
    "io.superflat.lagompb.readside.*",
    "io.superflat.lagompb.BaseServiceImpl",
    "io.superflat.lagompb.BaseApplication",
    "io.superflat.lagompb.BaseStatelessApplication",
    "io.superflat.lagompb.BaseGrpcServiceImpl",
    "io.superflat.lagompb.BaseServiceImpl",
    "io.superflat.lagompb.InternalServerError",
    "io.superflat.lagompb.LagompbPlugin",
    "io.superflat.lagompb.SharedBaseServiceImpl"
  )
}
