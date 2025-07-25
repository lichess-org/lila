package lila.web

import java.time.Duration

import com.typesafe.config.Config
import kamon.metric.*
import kamon.module.{ MetricReporter, Module, ModuleFactory }
import kamon.prometheus.*
import kamon.Kamon

// copy paste with some minor changes/scalafmtAll from https://github.com/kamon-io/Kamon/blob/master/reporters/kamon-prometheus/src/main/scala/kamon/prometheus/PrometheusReporter.scala
// unused http server has been removed
class PrometheusReporter(configPath: String = DefaultConfigPath, initialConfig: Config = Kamon.config())
    extends MetricReporter
    with ScrapeSource:

  import PrometheusReporter.readSettings
  import kamon.prometheus.PrometheusSettings.environmentTags

  private val stalePeriod = Duration.ofSeconds(2 * 24 * 60 * 60 + 1) // 2 days + 1 second
  private val _snapshotAccumulator =
    PeriodSnapshot.accumulator(stalePeriod, Duration.ZERO, stalePeriod)

  @volatile private var _preparedScrapeData: String =
    "# The kamon-prometheus module didn't receive any data just yet.\n"

  @volatile private var _reporterSettings = readSettings(initialConfig.getConfig(configPath))

  override def stop(): Unit =
    // Removes a reference to the last reporter to avoid leaking instances.
    //
    // It might not be safe to assume that **this** object is the last created instance, but in practice
    // users never have more than one instance running. If they do, they can handle access to their instances
    // by themselves.
    PrometheusReporter._lastCreatedInstance = None

  override def reconfigure(newConfig: Config): Unit =
    _reporterSettings = readSettings(newConfig.getConfig(configPath))

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

  case class Settings(generic: PrometheusSettings.Generic)

  def readSettings(prometheusConfig: Config): PrometheusReporter.Settings =
    PrometheusReporter.Settings(
      generic = PrometheusSettings.readSettings(prometheusConfig)
    )
