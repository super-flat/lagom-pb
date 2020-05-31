package lagompb.readside.utils

import com.typesafe.config.Config

case object LagompbProducerConfig {

  def apply(config: Config): LagompbProducerConfig =
    new LagompbProducerConfig(
      config.getString("bootstrap.servers"),
      config.getString("states.topic"),
      config.getString("events.topic")
    )
}

final class LagompbProducerConfig(val bootstrapServers: String, val stateTopic: String, val eventsTopic: String)
