package lagompb

import lagompb.protobuf.core.CoreProto
import lagompb.protobuf.options.OptionsProto
import lagompb.testkit.LagompbSpec
import lagompb.util.LagompbCommon
import scalapb.GeneratedFileObject

class LagompbCommonSpec extends LagompbSpec {
  "Loading GeneratedFileObject" should {
    "succeed" in {
      val fo: Seq[GeneratedFileObject] = LagompbCommon.loadFileObjects()
      fo.size should be >= 1
      fo should contain(OptionsProto)
      fo should contain(CoreProto)
    }
  }
}
