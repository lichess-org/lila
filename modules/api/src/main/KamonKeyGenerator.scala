package lila.api

import com.typesafe.config.Config
import kamon.statsd._
import kamon.metric.{ MetricKey, Entity }

// trying to organize metrics with dots
class KeepDotsMetricKeyGenerator(config: Config) extends SimpleMetricKeyGenerator(config) {

  override def createNormalizer(strategy: String): Normalizer = strategy match {
    case "keep-dots" => (s: String) ⇒ s.replace(": ", "-").replace(" ", "_").replace("/", "_") //.replace(".", "_")
    case other => super.createNormalizer(strategy)
  }

  override def generateKey(entity: Entity, metricKey: MetricKey): String =
    super.generateKey(entity, metricKey).pp
}
