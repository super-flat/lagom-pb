package io.superflat.lagompb

import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, TransportErrorCode, TransportException}

/**
 * It used to return an HTTP 500 response
 *
 * @param errorCode the http status code which is 500
 * @param exceptionMessage the error message
 * @param cause the http exception
 */
final class Http500(
    errorCode: TransportErrorCode,
    exceptionMessage: ExceptionMessage,
    cause: Throwable
) extends TransportException(errorCode, exceptionMessage, cause) {
  // $COVERAGE-OFF$
  def this(errorCode: TransportErrorCode, exceptionMessage: ExceptionMessage) =
    this(errorCode, exceptionMessage, new Throwable)
  // $COVERAGE-ON$
}

object Http500 {
  // $COVERAGE-OFF$
  val ErrorCode: TransportErrorCode = TransportErrorCode.InternalServerError

  def apply(message: String) =
    new Http500(
      ErrorCode,
      new ExceptionMessage(classOf[Http500].getSimpleName, message),
      new Throwable
    )

  def apply(cause: Throwable) =
    new Http500(
      ErrorCode,
      new ExceptionMessage(
        classOf[Http500].getSimpleName,
        cause.getMessage
      ),
      cause
    )
  // $COVERAGE-ON$
}
