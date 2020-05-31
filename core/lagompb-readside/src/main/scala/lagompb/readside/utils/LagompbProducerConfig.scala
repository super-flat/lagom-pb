package lagompb.readside.utils

import com.typesafe.config.Config

case object LagompbProducerConfig {

  def apply(config: Config): LagompbProducerConfig =
    new LagompbProducerConfig(config.getString("bootstrap.servers"), config.getString("topic"))
}

final class LagompbProducerConfig(val bootstrapServers: String, val topic: String)
