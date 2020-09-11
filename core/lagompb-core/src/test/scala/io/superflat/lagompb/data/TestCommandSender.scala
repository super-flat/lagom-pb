package io.superflat.lagompb.data

import io.superflat.lagompb.protobuf.v1.core.FailureResponse
import io.superflat.lagompb.protobuf.v1.core.FailureResponse.FailureType
import io.superflat.lagompb.{InvalidCommandException, LagompbException, NotFoundException, SendCommand}

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
      case FailureType.Critical(value)   => Failure(new LagompbException(value))
      case FailureType.Validation(value) => Failure(new InvalidCommandException(value))
      case FailureType.NotFound(value)   => Failure(new NotFoundException(value))
      case FailureType.Empty             => Failure(new LagompbException("reason unknown"))
      case _                             => Failure(new LagompbException("reason unknown"))
    }
  }

}
