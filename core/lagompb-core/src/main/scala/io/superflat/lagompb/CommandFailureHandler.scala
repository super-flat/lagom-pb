package io.superflat.lagompb
import com.google.protobuf.any.Any

import scala.util.Failure

/**
 * CommandFailureHandler is used to handle custom command handler errors
 * thrown while handling commands
 */
trait CommandFailureHandler {

  /**
   * Handles the custom error thrown in the command handler
   *
   * @param errorDetails the actual error to handle
   * @return  a Failure of type Try[Throwable]
   */
  def tryHandleError(errorDetails: Any): Failure[Throwable]
}
