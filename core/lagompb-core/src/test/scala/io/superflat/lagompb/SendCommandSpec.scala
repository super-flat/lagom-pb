/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import io.superflat.lagompb.data.TestCommandSender
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.tests.TestState
import io.superflat.lagompb.testkit.BaseSpec

import scala.util.Success

class SendCommandSpec extends BaseSpec {
  "handleLagompbCommandReply" should {

    "return a statewrapper on success" in {
      val someStateWrapper = StateWrapper().withState(Any.pack(StringValue("good")))

      val cmd = CommandReply().withStateWrapper(someStateWrapper)

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual shouldBe Success(someStateWrapper)
    }

    "run the provided transformFailedReply on failure" in {
      val failCause = "just coz"

      val cmd = CommandReply().withFailure(FailureResponse().withCritical(failCause))

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual.isFailure shouldBe true
      actual.failed.get.getMessage shouldBe failCause
    }

    "fails when unknown reply found" in {
      val cmd = CommandReply()

      val actual = TestCommandSender.handleLagompbCommandReply(cmd)

      actual.isFailure shouldBe true
      actual.failed.get.isInstanceOf[RuntimeException] shouldBe true
      actual.failed.get.getMessage.contains("unknown CommandReply") shouldBe true
    }
  }

  "transformFailedReply" should {
    "handle validation errors" in {
      val errMsg: String = "validation error"
      val actual = TestCommandSender.transformFailedReply(FailureResponse().withValidation(errMsg))

      actual.isFailure shouldBe true
      actual.failed.get.isInstanceOf[IllegalArgumentException] shouldBe true
      actual.failed.get.getMessage shouldBe errMsg
    }

    "handle internal errors" in {
      val errMsg: String = "internal error"
      val actual = TestCommandSender.transformFailedReply(FailureResponse().withCritical(errMsg))

      actual.isFailure shouldBe true
      actual.failed.get.isInstanceOf[RuntimeException] shouldBe true
      actual.failed.get.getMessage shouldBe errMsg
    }
  }

  "unpackStateWrapper" should {
    "unpack a known proto" in {
      val msg = TestState.defaultInstance
      val meta = MetaData().withRevisionNumber(1)
      val stateWrapper = StateWrapper().withState(Any.pack(msg)).withMeta(meta)
      val actual = TestCommandSender.unpackStateWrapper(stateWrapper)
      actual shouldBe Success((msg, meta))
    }

    "fail on unknown proto" in {
      val badAny = Any().withTypeUrl("whoopsidaisies")
      val stateWrapper = StateWrapper().withState(badAny)
      val actual = TestCommandSender.unpackStateWrapper(stateWrapper)
      actual.isFailure shouldBe true
      (actual.failure.exception should have).message("could not unpack unrecognized proto whoopsidaisies")
    }
  }
}
