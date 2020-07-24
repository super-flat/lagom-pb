package io.superflat.lagompb.readside

import io.superflat.lagompb.protobuf.core.MetaData

/**
 * Wraps events read from the journal and make it available to any consumer
 *
 * @param event the event
 * @param eventTag the event tag
 * @param state the resulting state of the event
 * @param metaData the additional metadata
 * @tparam TState the resulting state scala type
 */
case class ReadSideEvent[TState <: scalapb.GeneratedMessage](
    event: scalapb.GeneratedMessage,
    eventTag: String,
    state: TState,
    metaData: MetaData
)
