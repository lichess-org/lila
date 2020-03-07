package lila.api.prometheus

import kamon._
import java.time.Duration
import com.typesafe.config.{Config, ConfigUtil}
import kamon.metric._
import kamon.module.{MetricReporter, Module, ModuleFactory}
import kamon.tag.TagSet
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

final class PrometheusReporter(configPath: String) extends MetricReporter {
  import PrometheusReporter.Settings.{readSettings, environmentTags}

  private val _logger = LoggerFactory.getLogger(classOf[PrometheusReporter])
  private var _embeddedHttpServer: Option[EmbeddedHttpServer] = None
  private val _snapshotAccumulator = PeriodSnapshot.accumulator(Duration.ofDays(365 * 5), Duration.ZERO, Duration.ofDays(365 * 5))

  @volatile private var _preparedScrapeData: String =
    "# The kamon-prometheus module didn't receive any data just yet.\n"

  def this() =
    this("kamon.prometheus")

  {
    val initialSettings = readSettings(Kamon.config().getConfig(configPath))
    if(initialSettings.startEmbeddedServer)
      startEmbeddedServer(initialSettings)
  }

  override def stop(): Unit =
    stopEmbeddedServer()

  override def reconfigure(newConfig: Config): Unit = {
    val config = readSettings(newConfig.getConfig(configPath))

    stopEmbeddedServer()
    if(config.startEmbeddedServer) {
      startEmbeddedServer(config)
    }
  }

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    _snapshotAccumulator.add(snapshot)
    val currentData = _snapshotAccumulator.peek()
    val reporterConfiguration = readSettings(Kamon.config().getConfig(configPath))
    val scrapeDataBuilder = new ScrapeDataBuilder(reporterConfiguration, environmentTags(reporterConfiguration))

    scrapeDataBuilder.appendCounters(currentData.counters)
    scrapeDataBuilder.appendGauges(currentData.gauges)
    scrapeDataBuilder.appendHistograms(currentData.histograms)
    scrapeDataBuilder.appendHistograms(currentData.timers)
    scrapeDataBuilder.appendHistograms(currentData.rangeSamplers)
    _preparedScrapeData = scrapeDataBuilder.build()
  }

  def scrapeData(): String =
    _preparedScrapeData

  class EmbeddedHttpServer(hostname: String, port: Int) extends NanoHTTPD(hostname, port) {
    override def serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response = {
      newFixedLengthResponse(Response.Status.OK, "text/plain; version=0.0.4; charset=utf-8", scrapeData())
    }
  }

  private def startEmbeddedServer(config: PrometheusReporter.Settings): Unit = {
    val server = new EmbeddedHttpServer(config.embeddedServerHostname, config.embeddedServerPort)
    server.start()

    _logger.info(s"Started the embedded HTTP server on http://${config.embeddedServerHostname}:${config.embeddedServerPort}")
    _embeddedHttpServer = Some(server)
  }

  private def stopEmbeddedServer(): Unit =
    _embeddedHttpServer.foreach(_.stop())
}

object PrometheusReporter {

  class Factory extends ModuleFactory {
    override def create(settings: ModuleFactory.Settings): Module =
      new PrometheusReporter()
  }

  def create(): PrometheusReporter = {
    val defaultConfigPath = "kamon.prometheus"
    new PrometheusReporter()
  }


  case class Settings(
    startEmbeddedServer: Boolean,
    embeddedServerHostname: String,
    embeddedServerPort: Int,
    defaultBuckets: Seq[java.lang.Double],
    timeBuckets: Seq[java.lang.Double],
    informationBuckets: Seq[java.lang.Double],
    customBuckets: Map[String, Seq[java.lang.Double]],
    includeEnvironmentTags: Boolean
  )

  object Settings {

    def readSettings(prometheusConfig: Config): PrometheusReporter.Settings = {
      PrometheusReporter.Settings(
        startEmbeddedServer = prometheusConfig.getBoolean("start-embedded-http-server"),
        embeddedServerHostname = prometheusConfig.getString("embedded-server.hostname"),
        embeddedServerPort = prometheusConfig.getInt("embedded-server.port"),
        defaultBuckets = prometheusConfig.getDoubleList("buckets.default-buckets").asScala.toSeq,
        timeBuckets = prometheusConfig.getDoubleList("buckets.time-buckets").asScala.toSeq,
        informationBuckets = prometheusConfig.getDoubleList("buckets.information-buckets").asScala.toSeq,
        customBuckets = readCustomBuckets(prometheusConfig.getConfig("buckets.custom")),
        includeEnvironmentTags = prometheusConfig.getBoolean("include-environment-tags")
      )
    }

    def environmentTags(reporterConfiguration: PrometheusReporter.Settings): TagSet =
      if (reporterConfiguration.includeEnvironmentTags) Kamon.environment.tags else TagSet.Empty

    private def readCustomBuckets(customBuckets: Config): Map[String, Seq[java.lang.Double]] =
      customBuckets
        .topLevelKeys
        .map(k => (k, customBuckets.getDoubleList(ConfigUtil.quoteString(k)).asScala.toSeq))
        .toMap
  }
}
