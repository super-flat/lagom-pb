package lagompb

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
import lagompb.util.LagompbCommon

/**
 * LagomPbEvent used by lagom to tag events in the cluster
 */
trait LagompbEvent extends AggregateEvent[LagompbEvent] with scalapb.GeneratedMessage {
  def aggregateTag: AggregateEventTagger[LagompbEvent] = LagompbEvent.Tag
}

object LagompbEvent {
  val Tag: AggregateEventShards[LagompbEvent] =
    AggregateEventTag.sharded[LagompbEvent](
      LagompbCommon.config.getString("lagompb.events.tagname"),
      LagompbCommon.config.getInt("akka.cluster.sharding.number-of-shards")
    )
}
