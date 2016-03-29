package lila.common

object Chronometer {

  case class Lap[A](result: A, nanos: Long) {

    def millis = (nanos / 1000000).toInt

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      if (millis >= threshold) logger.debug(s"<${millis}ms> ${msg(result)}")
      this
    }
  }

  case class FuLap[A](lap: Fu[Lap[A]]) {

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      lap.foreach(_.logIfSlow(threshold, logger)(msg))
      this
    }

    def mon(path: lila.mon.RecPath) = {
      lap foreach { l =>
        lila.mon.recPath(path)(l.nanos)
      }
      this
    }

    def result = lap.map(_.result)
  }

  def apply[A](f: => Fu[A]): FuLap[A] = {
    val start = nowNanos
    FuLap(f map { Lap(_, nowNanos - start) })
  }

  def sync[A](f: => A): Lap[A] = {
    val start = nowNanos
    val res = f
    Lap(res, nowNanos - start)
  }

  def syncEffect[A](f: => A)(effect: Lap[A] => Unit): A = {
    val lap = sync(f)
    effect(lap)
    lap.result
  }
}
