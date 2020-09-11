package io.superflat.lagompb.readside

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.MetaData

/**
 * Wraps events read from the journal and make it available to any consumer
 *
 * @param event the event
 * @param eventTag the event tag
 * @param state the resulting state of the event
 * @param metaData the additional metadata
 */
case class ReadSideEvent(
    event: Any,
    eventTag: String,
    state: Any,
    metaData: MetaData
)
