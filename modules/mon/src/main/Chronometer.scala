package lila.mon

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import kamon.metric.Timer
import play.api.LoggerLike

import lila.core.lilaism.Lilaism.*

object Chronometer:

  object futureExtension:

    import scala.concurrent.Await
    extension [A](fua: Future[A])

      def await(duration: FiniteDuration, name: String): A =
        Chronometer.syncMon(lila.mon.blocking.time(name)):
          try Await.result(fua, duration)
          catch
            case e: Exception =>
              lila.mon.blocking.timeout(name).increment()
              throw e
      def awaitOrElse(duration: FiniteDuration, name: String, default: => A): A =
        try await(duration, name)
        catch case _: Exception => default

      def chronometer = Chronometer(fua)
      def chronometerTry = Chronometer.lapTry(fua)

      def mon(path: Timer): Fu[A] = chronometer.mon(path).result
      def monTry(path: scala.util.Try[A] => Timer): Fu[A] =
        chronometerTry.mon(r => path(r)).result
      def monSuccess(path: Boolean => kamon.metric.Timer): Fu[A] =
        chronometerTry
          .mon: r =>
            path(r.isSuccess)
          .result
      def monValue(path: A => Timer): Fu[A] = chronometer.monValue(path).result

      def logTime(name: String): Fu[A] = chronometer.pp(name)
      def logTimeIfGt(name: String, duration: FiniteDuration): Fu[A] = chronometer.ppIfGt(name, duration)
  end futureExtension

  case class Lap[A](result: A, nanos: Long):

    def millis = (nanos / 1000000).toInt
    def micros = (nanos / 1000).toInt
    def seconds = (millis / 1000).toInt

    def logIfSlow(threshold: Int, logger: LoggerLike)(msg: A => String) =
      if millis >= threshold then log(logger)(msg)
      else this
    def log(logger: LoggerLike)(msg: A => String) =
      logger.info(s"<${millis}ms> ${msg(result)}")
      this

    def mon(path: Timer) =
      path.record(nanos)
      this

    def monValue(path: A => Timer) =
      path(result).record(nanos)
      this

    def pp: A =
      println(s"chrono $showDuration")
      result

    def pp(msg: String): A =
      println(s"chrono $msg - $showDuration")
      result
    def ppIfGt(msg: String, duration: FiniteDuration): A =
      if nanos > duration.toNanos then pp(msg)
      else result

    def showDuration: String = if millis >= 1 then s"$millis ms" else s"$micros micros"

  case class LapTry[A](result: scala.util.Try[A], nanos: Long):
    def millis = (nanos / 1000000).toInt

  case class FuLap[A](lap: Fu[Lap[A]]) extends AnyVal:

    def logIfSlow(threshold: Int, logger: LoggerLike)(msg: A => String) =
      lap.dforeach(_.logIfSlow(threshold, logger)(msg))
      this

    def mon(path: Timer) =
      lap.dforeach(_.mon(path))
      this

    def monValue(path: A => Timer) =
      lap.dforeach(_.monValue(path))
      this

    def log(logger: LoggerLike)(msg: A => String) =
      lap.dforeach(_.log(logger)(msg))
      this

    def pp: Fu[A] = lap.dmap(_.pp)
    def pp(msg: String): Fu[A] = lap.dmap(_.pp(msg))
    def ppIfGt(msg: String, duration: FiniteDuration): Fu[A] = lap.dmap(_.ppIfGt(msg, duration))

    def tap(f: Lap[A] => Unit) =
      lap.dforeach(f)
      this

    def result = lap.dmap(_.result)

  case class FuLapTry[A](lap: Fu[LapTry[A]]) extends AnyVal:

    def mon(path: scala.util.Try[A] => kamon.metric.Timer) =
      lap.dforeach: l =>
        path(l.result).record(l.nanos)
      this

    def result =
      lap.flatMap { l =>
        Future.fromTry(l.result)
      }(using scala.concurrent.ExecutionContext.parasitic)

  def apply[A](f: Fu[A]): FuLap[A] =
    val start = nowNanosRel
    FuLap(f.dmap { Lap(_, nowNanosRel - start) })

  def lapTry[A](f: Fu[A]): FuLapTry[A] =
    val start = nowNanosRel
    FuLapTry:
      f.transformWith { r =>
        fuccess(LapTry(r, nowNanosRel - start))
      }(using scala.concurrent.ExecutionContext.parasitic)

  def sync[A](f: => A): Lap[A] =
    val start = nowNanosRel
    val res = f
    Lap(res, nowNanosRel - start)

  def syncEffect[A](f: => A)(effect: Lap[A] => Unit): A =
    val lap = sync(f)
    effect(lap)
    lap.result

  def syncMon[A](path: Timer)(f: => A): A =
    val timer = path.start()
    val res = f
    timer.stop()
    res

  def start =
    val at = nowNanosRel
    () => Lap((), nowNanosRel - at)
