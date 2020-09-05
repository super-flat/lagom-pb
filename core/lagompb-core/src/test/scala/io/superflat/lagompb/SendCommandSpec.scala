package io.superflat.lagompb

import io.superflat.lagompb.testkit.BaseSpec
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import io.superflat.lagompb.protobuf.v1.core.CommandReply
import io.superflat.lagompb.protobuf.v1.core.SuccessfulReply
import io.superflat.lagompb.protobuf.v1.core.StateWrapper
import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import io.superflat.lagompb.data.TestCommandSender
import scala.util.Success
import io.superflat.lagompb.protobuf.v1.core.FailedReply
import io.superflat.lagompb.protobuf.v1.core.FailureCause
import io.superflat.lagompb.protobuf.v1.core.MetaData

class SendCommandSpec extends BaseSpec {
  "handleLagompbCommandReply" should {
    "return a statewrapper on success" in {
      val someStateWrapper = StateWrapper()
        .withState(Any.pack(StringValue("good")))

      val cmd = CommandReply()
        .withSuccessfulReply(
          SuccessfulReply()
            .withStateWrapper(someStateWrapper)
        )

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual shouldBe (Success(someStateWrapper))

    }
    "run the provided transformFailedReply on failure" in {
      val failCause = "just coz"

      val cmd = CommandReply()
        .withFailedReply(
          FailedReply()
            .withCause(FailureCause.INTERNAL_ERROR)
            .withReason(failCause)
        )

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual.isFailure shouldBe (true)
      actual.failed.get.getMessage() shouldBe (failCause)
    }
    "fails when unknown reply found" in {
      val cmd = CommandReply()

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual.isFailure shouldBe (true)
      actual.failed.get.isInstanceOf[GlobalException] shouldBe(true)
      actual.failed.get.getMessage().contains("unknown CommandReply") shouldBe (true)
    }
  }

  "transformFailedReply" should {
    "handle validation errors" in {
      val errMsg: String = "validation error"
      val actual = TestCommandSender.transformFailedReply(
        FailedReply()
          .withCause(FailureCause.VALIDATION_ERROR)
          .withReason(errMsg)
      )

      actual.isFailure shouldBe (true)
      actual.failed.get.isInstanceOf[InvalidCommandException] shouldBe(true)
      actual.failed.get.getMessage shouldBe (errMsg)
    }
    "handle internal errors" in {
      val errMsg: String = "internal error"
      val actual = TestCommandSender.transformFailedReply(
        FailedReply()
          .withCause(FailureCause.INTERNAL_ERROR)
          .withReason(errMsg)
      )

      actual.isFailure shouldBe (true)
      actual.failed.get.isInstanceOf[GlobalException] shouldBe(true)
      actual.failed.get.getMessage shouldBe (errMsg)
    }
    "handle unknown errors" in {
      val actual = TestCommandSender.transformFailedReply(
        FailedReply()
      )

      actual.isFailure shouldBe (true)
      actual.failed.get.isInstanceOf[GlobalException] shouldBe(true)
      actual.failed.get.getMessage shouldBe ("reason unknown")
    }
  }

  "unpackStateWrapper" should {
    "unpack a known proto" in {
      val msg = StringValue("oh yeah")
      val meta = MetaData().withRevisionNumber(1L)
      val stateWrapper = StateWrapper().withState(Any.pack(msg)).withMeta(meta)
      val actual = TestCommandSender.unpackStateWrapper(stateWrapper)
      actual shouldBe(Success((msg, meta)))
    }
    "fail on unknown proto" in {
      val badAny = Any().withTypeUrl("whoopsidaisies")
      val stateWrapper = StateWrapper().withState(badAny)
      val actual = TestCommandSender.unpackStateWrapper(stateWrapper)
      actual.isFailure shouldBe(true)
      actual.failed.get.getMessage() shouldBe("state not found")
    }
  }
}
