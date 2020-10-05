/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, TransportErrorCode, TransportException}

/**
 * It used to return an HTTP 500 response
 * e http status code which is 500
 * @param exceptionMessage the error message
 * @param cause the http exception
 */
final class Http500(
    exceptionMessage: ExceptionMessage,
    cause: Throwable
) extends TransportException(TransportErrorCode.InternalServerError, exceptionMessage, cause) {
  // $COVERAGE-OFF$
  def this(errorCode: TransportErrorCode, exceptionMessage: ExceptionMessage) =
    this(exceptionMessage, new Throwable)
  // $COVERAGE-ON$
}

object Http500 {
  // $COVERAGE-OFF$
  val ErrorCode: TransportErrorCode = TransportErrorCode.InternalServerError

  def apply(message: String) =
    new Http500(
      new ExceptionMessage(classOf[Http500].getSimpleName, message),
      new Throwable
    )

  def apply(cause: Throwable) =
    new Http500(
      new ExceptionMessage(
        classOf[Http500].getSimpleName,
        cause.getMessage
      ),
      cause
    )
  // $COVERAGE-ON$
}
