package io.superflat.lagompb.data

import io.superflat.lagompb.SendCommand
import io.superflat.lagompb.protobuf.v1.core.FailureResponse
import io.superflat.lagompb.protobuf.v1.core.FailureResponse.FailureType

import scala.util.Failure

object TestCommandSender extends SendCommand {

  /**
   * generic conversion for failed replys into a scala Failure
   *
   * @param failureResponse some command handler failed reply
   * @return a Failure of type Try[]
   */
  override def transformFailedReply(failureResponse: FailureResponse): Failure[Throwable] = {
    failureResponse.failureType match {
      case FailureType.Critical(value)   => Failure(new RuntimeException(value))
      case FailureType.Validation(value) => Failure(new IllegalArgumentException(value))
      case FailureType.NotFound(value)   => Failure(new NoSuchElementException(value))
      case FailureType.Empty             => Failure(new RuntimeException("reason unknown"))
      case _                             => Failure(new RuntimeException("reason unknown"))
    }
  }

}
