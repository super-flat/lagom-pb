package io.superflat.lagompb.readside

import akka.Done
import com.google.protobuf.any
import io.superflat.lagompb.protobuf.v1.core.MetaData
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.dbio.DBIO

/**
 * Helps process the events read from the journal
 */
trait EventProcessor {

  /**
   * Processes events read from the Journal
   *
   * @param comp the scalapb companion object used to unmarshall the aggregate state
   * @param event the actual event
   * @param eventTag the event tag
   * @param resultingState the resulting state of the applied event
   * @param meta the additional meta data
   * @return
   */
  def process(
    comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
    event: any.Any,
    eventTag: String,
    resultingState: any.Any,
    meta: MetaData
  ): DBIO[Done]
}
