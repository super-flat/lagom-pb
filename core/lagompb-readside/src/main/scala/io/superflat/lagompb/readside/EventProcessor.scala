package io.superflat.lagompb.readside

import akka.Done
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.MetaData
import slick.dbio.DBIO

trait EventProcessor {

  /**
   * Processes events read from the Journal
   *
   * @param event the actual event
   * @param eventTag the event tag
   * @param resultingState the resulting state of the applied event
   * @param meta the additional meta data
   * @return
   */
  def process(
      event: Any,
      eventTag: String,
      resultingState: Any,
      meta: MetaData
  ): DBIO[Done]
}
