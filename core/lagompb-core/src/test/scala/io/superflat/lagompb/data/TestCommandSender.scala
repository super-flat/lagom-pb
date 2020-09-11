package io.superflat.lagompb.data

import io.superflat.lagompb.{CommandFailureHandler, SendCommand}

object TestCommandSender extends SendCommand {

  /**
   * Custom Command Error handler that needs to be implemented.
   */
  override def commandFailureHandler: Option[CommandFailureHandler] = None
}
