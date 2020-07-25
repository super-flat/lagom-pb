package io.superflat.lagompb

import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, TransportErrorCode, TransportException}

/**
 * It used to return an HTTP 500 response
 *
 * @param errorCode the http status code which is 500
 * @param exceptionMessage the error message
 * @param cause the http exception
 */
final class InternalServerError(errorCode: TransportErrorCode, exceptionMessage: ExceptionMessage, cause: Throwable)
    extends TransportException(errorCode, exceptionMessage, cause) {
  // $COVERAGE-OFF$
  def this(errorCode: TransportErrorCode, exceptionMessage: ExceptionMessage) =
    this(errorCode, exceptionMessage, null)
  // $COVERAGE-ON$
}

object InternalServerError {
  // $COVERAGE-OFF$
  val ErrorCode: TransportErrorCode = TransportErrorCode.InternalServerError

  def apply(message: String) =
    new InternalServerError(ErrorCode, new ExceptionMessage(classOf[InternalServerError].getSimpleName, message), null)

  def apply(cause: Throwable) =
    new InternalServerError(
      ErrorCode,
      new ExceptionMessage(classOf[InternalServerError].getSimpleName, cause.getMessage),
      cause
    )
  // $COVERAGE-ON$
}

/**
 *  GlobalException custom RuntimeException.
 *  This is used for general exception
 *
 * @param message the exception message
 */
final class GlobalException(message: String) extends RuntimeException(message)

/**
 * InvalidCommandException.
 * This will be used to handle invalid command exception.
 * @param message the exception message
 */
final class InvalidCommandException(message: String) extends RuntimeException(message)
