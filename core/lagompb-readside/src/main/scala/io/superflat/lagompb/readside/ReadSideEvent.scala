package io.superflat.lagompb.readside

import io.superflat.lagompb.v1.protobuf.core.MetaData

/**
 * Wraps events read from the journal and make it available to any consumer
 *
 * @param event the event
 * @param eventTag the event tag
 * @param state the resulting state of the event
 * @param metaData the additional metadata
 * @tparam S the resulting state scala type
 */
case class ReadSideEvent[S <: scalapb.GeneratedMessage](
  event: scalapb.GeneratedMessage,
  eventTag: String,
  state: S,
  metaData: MetaData
)
