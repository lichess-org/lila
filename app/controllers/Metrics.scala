package controllers

import lila.app.Env
import kamon.prometheus.PrometheusReporter

final class Metrics(env: Env) extends LilaController(env):

  def metrics = OpenBody:
    PrometheusReporter.latestScrapeData().fold(NotFound("No metrics found"))(Ok(_))
