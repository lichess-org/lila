package lila.common

import scala.concurrent.duration.FiniteDuration

object Chronometer {

  case class Lap[A](result: A, nanos: Long) {

    def millis = (nanos / 1000000).toInt
    def micros = (nanos / 1000).toInt

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      if (millis >= threshold) logger.debug(s"<${millis}ms> ${msg(result)}")
      this
    }

    def pp: A = {
      println(s"chrono $showDuration")
      result
    }

    def pp(msg: String): A = {
      println(s"chrono $msg - $showDuration")
      result
    }
    def ppIfGt(msg: String, duration: FiniteDuration): A =
      if (nanos > duration.toNanos) pp(msg)
      else result

    def showDuration: String = if (millis >= 1) f"$millis%.2f ms" else s"$micros micros"
  }

  case class FuLap[A](lap: Fu[Lap[A]]) extends AnyVal {

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      lap.dforeach(_.logIfSlow(threshold, logger)(msg))
      this
    }

    def mon(path: lila.mon.RecPath) = {
      lap dforeach { l =>
        lila.mon.recPath(path)(l.nanos)
      }
      this
    }

    def pp: Fu[A] = lap dmap (_.pp)
    def pp(msg: String): Fu[A] = lap dmap (_ pp msg)
    def ppIfGt(msg: String, duration: FiniteDuration): Fu[A] = lap dmap (_.ppIfGt(msg, duration))

    def result = lap.dmap(_.result)
  }

  def apply[A](f: => Fu[A]): FuLap[A] = {
    val start = nowNanos
    FuLap(f dmap { Lap(_, nowNanos - start) })
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

  def syncMon[A](path: lila.mon.RecPath)(f: => A): A = {
    val start = nowNanos
    val res = f
    lila.mon.recPath(path)(nowNanos - start)
    res
  }

  def start = new {
    private val s = nowNanos
    def mon(path: lila.mon.RecPath): Unit = lila.mon.recPath(path)(nowNanos - s)
  }
}
