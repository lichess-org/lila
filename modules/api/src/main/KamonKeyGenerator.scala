package lila.api

import kamon.statsd._
import com.typesafe.config.Config

// trying to organize metrics with dots
class KeepDotsMetricKeyGenerator(config: Config) extends SimpleMetricKeyGenerator(config) {

  override def createNormalizer(strategy: String): Normalizer = strategy match {
    case "keep-dots" => (s: String) ⇒ s.replace(": ", "-").replace(" ", "_").replace("/", "_") //.replace(".", "_")
    case other => super.createNormalizer(strategy)
  }
}
