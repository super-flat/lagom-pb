package io.superflat.lagompb.readside

import com.typesafe.config.Config

case object KafkaConfig {

  def apply(config: Config): KafkaConfig =
    new KafkaConfig(
      config.getString("bootstrap.servers"),
      config.getString("topic")
    )
}

final class KafkaConfig(val bootstrapServers: String, val topic: String)
