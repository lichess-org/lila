package lila.app
package prometheus

import java.time.Duration

import com.typesafe.config.Config
import kamon.metric.*
import kamon.module.{ MetricReporter, Module, ModuleFactory }
import kamon.prometheus.PrometheusReporter.DefaultConfigPath
import kamon.prometheus.embeddedhttp.EmbeddedHttpServer
import kamon.prometheus.*
import kamon.Kamon

import org.slf4j.LoggerFactory

// copy paste with some minor changes/scalafmtAll from https://github.com/kamon-io/Kamon/blob/master/reporters/kamon-prometheus/src/main/scala/kamon/prometheus/PrometheusReporter.scala
class PrometheusReporter(configPath: String = DefaultConfigPath, initialConfig: Config = Kamon.config())
    extends MetricReporter
    with ScrapeSource:

  import PrometheusReporter.Settings.readSettings
  import kamon.prometheus.PrometheusSettings.environmentTags

  private val _logger = LoggerFactory.getLogger(classOf[PrometheusReporter])
  private var _embeddedHttpServer: Option[EmbeddedHttpServer] = None

  private val stalePeriod = Duration.ofSeconds(2 * 24 * 60 * 60 + 1) // 2 days + 1 second
  private val _snapshotAccumulator =
    PeriodSnapshot.accumulator(stalePeriod, Duration.ZERO, stalePeriod)

  @volatile private var _preparedScrapeData: String =
    "# The kamon-prometheus module didn't receive any data just yet.\n"

  @volatile private var _config           = initialConfig
  @volatile private var _reporterSettings = readSettings(initialConfig.getConfig(configPath))

  override def stop(): Unit =
    stopEmbeddedServerIfStarted()

    // Removes a reference to the last reporter to avoid leaking instances.
    //
    // It might not be safe to assume that **this** object is the last created instance, but in practice
    // users never have more than one instance running. If they do, they can handle access to their instances
    // by themselves.
    PrometheusReporter._lastCreatedInstance = None

  override def reconfigure(newConfig: Config): Unit =
    _reporterSettings = readSettings(newConfig.getConfig(configPath))
    _config = newConfig
    stopEmbeddedServerIfStarted()
    startEmbeddedServerIfEnabled()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    _snapshotAccumulator.add(snapshot)
    val currentData = _snapshotAccumulator.peek()
    val scrapeDataBuilder =
      new ScrapeDataBuilder(_reporterSettings.generic, environmentTags(_reporterSettings.generic))

    scrapeDataBuilder.appendCounters(currentData.counters)
    scrapeDataBuilder.appendGauges(currentData.gauges)
    scrapeDataBuilder.appendDistributionMetricsAsGauges(
      snapshot.rangeSamplers ++ snapshot.histograms ++ snapshot.timers
    )
    scrapeDataBuilder.appendHistograms(currentData.histograms)
    scrapeDataBuilder.appendHistograms(currentData.timers)
    scrapeDataBuilder.appendHistograms(currentData.rangeSamplers)
    _preparedScrapeData = scrapeDataBuilder.build()

  def scrapeData(): String =
    _preparedScrapeData

  private def startEmbeddedServerIfEnabled(): Unit =
    if _reporterSettings.startEmbeddedServer then
      val server = _reporterSettings
        .createEmbeddedHttpServerClass()
        .getConstructor(
          Array[Class[?]](
            classOf[String],
            classOf[Int],
            classOf[String],
            classOf[ScrapeSource],
            classOf[Config]
          )*
        )
        .newInstance(
          _reporterSettings.embeddedServerHostname,
          _reporterSettings.embeddedServerPort: Integer,
          _reporterSettings.embeddedServerPath,
          this,
          _config
        )
      _logger.info(
        s"Started the embedded HTTP server on http://${_reporterSettings.embeddedServerHostname}:${_reporterSettings.embeddedServerPort}${_reporterSettings.embeddedServerPath}"
      )
      _embeddedHttpServer = Some(server)

  private def stopEmbeddedServerIfStarted(): Unit =
    _embeddedHttpServer.foreach(_.stop())

object PrometheusReporter:

  final val DefaultConfigPath = "kamon.prometheus"

  /** We keep a reference to the last created Prometheus Reporter instance so that users can easily access it
    * if they want to publish the scrape data through their own HTTP server.
    */
  @volatile private var _lastCreatedInstance: Option[PrometheusReporter] = None

  /** Returns the latest Prometheus scrape data created by the latest PrometheusReporter instance created
    * automatically by Kamon. If you are creating more than one PrometheusReporter instance you might prefer
    * to keep references to those instances programmatically and calling `.scrapeData()` directly on them
    * instead of using this function.
    */
  def latestScrapeData(): Option[String] =
    _lastCreatedInstance.map(_.scrapeData())

  class Factory extends ModuleFactory:
    override def create(settings: ModuleFactory.Settings): Module =
      val reporter = new PrometheusReporter(DefaultConfigPath, settings.config)
      _lastCreatedInstance = Some(reporter)
      reporter

  def create(): PrometheusReporter =
    new PrometheusReporter()

  case class Settings(
      startEmbeddedServer: Boolean,
      embeddedServerHostname: String,
      embeddedServerPort: Int,
      embeddedServerPath: String,
      embeddedServerImpl: String,
      generic: PrometheusSettings.Generic
  ):
    def createEmbeddedHttpServerClass(): Class[? <: EmbeddedHttpServer] =
      val clz = embeddedServerImpl match
        case "sun" => "kamon.prometheus.embeddedhttp.SunEmbeddedHttpServer"
        case fqcn  => fqcn
      Class.forName(clz).asInstanceOf[Class[? <: EmbeddedHttpServer]]

  object Settings:

    def readSettings(prometheusConfig: Config): PrometheusReporter.Settings =
      PrometheusReporter.Settings(
        startEmbeddedServer = prometheusConfig.getBoolean("start-embedded-http-server"),
        embeddedServerHostname = prometheusConfig.getString("embedded-server.hostname"),
        embeddedServerPort = prometheusConfig.getInt("embedded-server.port"),
        embeddedServerPath = prometheusConfig.getString("embedded-server.metrics-path"),
        embeddedServerImpl = prometheusConfig.getString("embedded-server.impl"),
        generic = PrometheusSettings.readSettings(prometheusConfig)
      )
