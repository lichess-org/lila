package lila.common

object Chronometer {

  case class Lap[A](result: A, millis: Int) {

    def logIfSlow(threshold: Int, logger: String)(msg: A => String) = {
      if (millis >= threshold) play.api.Logger(logger).debug(s"<${millis}ms> ${msg(result)}")
      this
    }
  }

  case class FuLap[A](lap: Fu[Lap[A]]) {

    def logIfSlow(threshold: Int, logger: String)(msg: A => String) = {
      lap.foreach(_.logIfSlow(threshold, logger)(msg))
      this
    }

    def mon(path: lila.mon.RecPath) = {
      lap foreach { l =>
        lila.mon.recPath(path)(l.millis)
      }
      this
    }

    def result = lap.map(_.result)
  }

  def apply[A](f: => Fu[A]): FuLap[A] = {
    val start = nowMillis
    FuLap(f map {
      Lap(_, (nowMillis - start).toInt)
    })
  }
}
