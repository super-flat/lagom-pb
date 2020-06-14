package lagompb

import com.lightbend.lagom.scaladsl.persistence.{
  AggregateEvent,
  AggregateEventShards,
  AggregateEventTag,
  AggregateEventTagger
}

/**
 * LagomPbEvent used by lagom to tag events in the cluster
 */
trait LagompbEvent extends AggregateEvent[LagompbEvent] with scalapb.GeneratedMessage {
  def aggregateTag: AggregateEventTagger[LagompbEvent] = LagompbEvent.Tag
}

object LagompbEvent {

  val Tag: AggregateEventShards[LagompbEvent] =
    AggregateEventTag.sharded[LagompbEvent](LagompbConfig.eventsConfig.tagName, LagompbConfig.eventsConfig.numShards)
}
