package lila.common

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object Chronometer {

  case class Lap[A](result: A, nanos: Long) {

    def millis = (nanos / 1000000).toInt
    def micros = (nanos / 1000).toInt

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      if (millis >= threshold) log(logger)(msg)
      else this
    }
    def log(logger: lila.log.Logger)(msg: A => String) = {
      logger.info(s"<${millis}ms> ${msg(result)}")
      this
    }

    def mon(path: lila.mon.TimerPath) = {
      path(lila.mon).record(nanos).unit
      this
    }

    def monValue(path: A => lila.mon.TimerPath) = {
      path(result)(lila.mon).record(nanos).unit
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

    def showDuration: String = if (millis >= 1) s"$millis ms" else s"$micros micros"
  }
  case class LapTry[A](result: Try[A], nanos: Long)

  case class FuLap[A](lap: Fu[Lap[A]]) extends AnyVal {

    def logIfSlow(threshold: Int, logger: lila.log.Logger)(msg: A => String) = {
      lap.dforeach(_.logIfSlow(threshold, logger)(msg).unit)
      this
    }

    def mon(path: lila.mon.TimerPath) = {
      lap dforeach { _.mon(path).unit }
      this
    }

    def monValue(path: A => lila.mon.TimerPath) = {
      lap dforeach { _.monValue(path).unit }
      this
    }

    def log(logger: lila.log.Logger)(msg: A => String) = {
      lap.dforeach(_.log(logger)(msg).unit)
      this
    }

    def pp: Fu[A]                                            = lap.dmap(_.pp)
    def pp(msg: String): Fu[A]                               = lap.dmap(_ pp msg)
    def ppIfGt(msg: String, duration: FiniteDuration): Fu[A] = lap.dmap(_.ppIfGt(msg, duration))

    def tap(f: Lap[A] => Unit) = {
      lap dforeach f
      this
    }

    def result = lap.dmap(_.result)
  }

  case class FuLapTry[A](lap: Fu[LapTry[A]]) extends AnyVal {

    def mon(path: Try[A] => kamon.metric.Timer) = {
      lap.dforeach { l =>
        path(l.result).record(l.nanos).unit
      }
      this
    }

    def result =
      lap.flatMap { l =>
        Future.fromTry(l.result)
      }(ExecutionContext.parasitic)
  }

  def apply[A](f: Fu[A]): FuLap[A] = {
    val start = nowNanos
    FuLap(f dmap { Lap(_, nowNanos - start) })
  }

  def lapTry[A](f: Fu[A]): FuLapTry[A] = {
    val start = nowNanos
    FuLapTry {
      f.transformWith { r =>
        fuccess(LapTry(r, nowNanos - start))
      }(ExecutionContext.parasitic)
    }
  }

  def sync[A](f: => A): Lap[A] = {
    val start = nowNanos
    val res   = f
    Lap(res, nowNanos - start)
  }

  def syncEffect[A](f: => A)(effect: Lap[A] => Unit): A = {
    val lap = sync(f)
    effect(lap)
    lap.result
  }

  def syncMon[A](path: lila.mon.TimerPath)(f: => A): A = {
    val timer = path(lila.mon).start()
    val res   = f
    timer.stop()
    res
  }
}
