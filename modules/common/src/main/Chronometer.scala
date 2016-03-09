package lila.common

import kamon.Kamon.metrics

object Chronometer {

  case class Lap[A](result: A, millis: Int) {
    def tuple = result -> millis

    def logIfSlow(threshold: Int, logger: String)(msg: A => String) = {
      if (millis >= threshold) play.api.Logger(logger).debug(s"<${millis}ms> ${msg(result)}")
      this
    }

    def kamon(histogram: String) = {
      metrics.histogram(name) record millis
      this
    }
  }

  def log[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    logger debug s"$name - start"
    f.addEffects(
      err => logger warn s"$name - failed in ${nowMillis - start}ms - ${err.getMessage}",
      _ => logger debug s"$name - done in ${nowMillis - start}ms")
  }

  def result[A](f: => Fu[A]): Fu[Lap[A]] = {
    val start = nowMillis
    f map {
      Lap(_, (nowMillis - start).toInt)
    }
  }

  private lazy val logger = play.api.Logger("chrono")
}
