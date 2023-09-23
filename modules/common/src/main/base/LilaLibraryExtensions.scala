package lila.base

import alleycats.Zero
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import java.lang.Math.{ max, min }
import scala.concurrent.{ Await, ExecutionContext as EC }
import scala.util.matching.Regex
import scala.util.Try

import java.util.Base64
import lila.common.Chronometer
import ornicar.scalalib.extensions.*
import scala.annotation.targetName

trait LilaLibraryExtensions extends LilaTypes:

  extension (self: Long)
    def atLeast(bottomValue: Long): Long       = max(self, bottomValue)
    def atMost(topValue: Long): Long           = min(self, topValue)
    def squeeze(bottom: Long, top: Long): Long = max(min(self, top), bottom)
    def toSaturatedInt: Int =
      if self.toInt == self then self.toInt
      else if self > 0 then Integer.MAX_VALUE
      else Integer.MIN_VALUE

  extension (self: Int)
    def atLeast(bottomValue: Int): Int      = max(self, bottomValue)
    def atMost(topValue: Int): Int          = min(self, topValue)
    def squeeze(bottom: Int, top: Int): Int = max(min(self, top), bottom)

  extension (self: Float)
    def atLeast(bottomValue: Float): Float        = max(self, bottomValue)
    def atMost(topValue: Float): Float            = min(self, topValue)
    def squeeze(bottom: Float, top: Float): Float = max(min(self, top), bottom)

  extension (self: Double)
    def atLeast(bottomValue: Double): Double         = max(self, bottomValue)
    def atMost(topValue: Double): Double             = min(self, topValue)
    def squeeze(bottom: Double, top: Double): Double = max(min(self, top), bottom)

  extension [A](self: Option[A])

    def toTryWith(err: => Exception): Try[A] =
      self.fold[Try[A]](scala.util.Failure(err))(scala.util.Success.apply)

    def toTry(err: => String): Try[A] = toTryWith(lila.base.LilaException(err))

    def err(message: => String): A = self.getOrElse(sys.error(message))

    // move to scalalib? generalize Future away?
    def soFu[B](f: A => Future[B]): Future[Option[B]] = self match
      case Some(x) => f(x).map(Some(_))(scala.concurrent.ExecutionContext.parasitic)
      case None    => Future.successful(None)

  extension (self: Boolean)
    // move to scalalib? generalize Future away?
    def soFu[B](f: => Future[B]): Future[Option[B]] =
      if self then f.map(Some(_))(scala.concurrent.ExecutionContext.parasitic)
      else Future.successful(None)

  extension (s: String)

    def replaceIf(t: Char, r: Char): String =
      if s.indexOf(t.toInt) >= 0 then s.replace(t, r) else s

    def replaceIf(t: Char, r: CharSequence): String =
      if s.indexOf(t.toInt) >= 0 then s.replace(String.valueOf(t), r) else s

    def replaceIf(t: CharSequence, r: CharSequence): String =
      if s.contains(t) then s.replace(t, r) else s

    def replaceAllIn(regex: Regex, replacement: String) = regex.replaceAllIn(s, replacement)

  extension (config: Config)
    def millis(name: String): Int              = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
    def seconds(name: String): Int             = config.getDuration(name, TimeUnit.SECONDS).toInt
    def duration(name: String): FiniteDuration = millis(name).millis

  extension [A, B](v: Either[A, B])
    def toFuture: Fu[B] = v match
      case Right(res) => fuccess(res)
      case Left(err) =>
        err match
          case e: Exception => Future.failed(e)
          case _            => fufail(err.toString)

  extension [A, B](v: (A, B)) def map2[C](f: B => C): (A, C) = (v._1, f(v._2))

  extension (d: FiniteDuration)
    def toCentis = chess.Centis(d)
    def abs      = if d.length < 0 then -d else d

  extension [A](list: List[A])
    def sortLike[B](other: Seq[B], f: A => B): List[A] =
      list.sortWith { (x, y) =>
        other.indexOf(f(x)) < other.indexOf(f(y))
      }
    def tailOption: Option[List[A]] = list match
      case Nil       => None
      case _ :: rest => Some(rest)
    def tailSafe: List[A]         = tailOption getOrElse Nil
    def indexOption(a: A)         = Option(list indexOf a).filter(0 <= _)
    def previous(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i - 1))
    def next(a: A): Option[A]     = indexOption(a).flatMap(i => list.lift(i + 1))

  extension (self: Array[Byte]) def toBase64 = Base64.getEncoder.encodeToString(self)

  // run a collection of futures in parallel
  extension [A](list: List[Fu[A]]) def parallel(using Executor): Fu[List[A]]         = Future sequence list
  extension [A](vec: Vector[Fu[A]]) def parallel(using Executor): Fu[Vector[A]]      = Future sequence vec
  extension [A](set: Set[Fu[A]]) def parallel(using Executor): Fu[Set[A]]            = Future sequence set
  extension [A](seq: Seq[Fu[A]]) def parallel(using Executor): Fu[Seq[A]]            = Future sequence seq
  extension [A](iter: Iterable[Fu[A]]) def parallel(using Executor): Fu[Iterable[A]] = Future sequence iter
  extension [A](iter: Iterator[Fu[A]]) def parallel(using Executor): Fu[Iterator[A]] = Future sequence iter

  extension [A](fua: Fu[A])

    inline def dmap[B](f: A => B): Fu[B]       = fua.map(f)(EC.parasitic)
    inline def dforeach[B](f: A => Unit): Unit = fua.foreach(f)(EC.parasitic)

    def andDo(sideEffect: => Unit)(using Executor): Fu[A] =
      fua.andThen:
        case _ => sideEffect

    def >>[B](fub: => Fu[B])(using Executor): Fu[B] =
      fua.flatMap(_ => fub)

    inline def void: Fu[Unit] =
      dmap(_ => ())

    inline def inject[B](b: => B): Fu[B] =
      dmap(_ => b)

    def injectAnyway[B](b: => B)(using Executor): Fu[B] = fold(_ => b, _ => b)

    def effectFold(fail: Exception => Unit, succ: A => Unit)(using Executor): Unit =
      fua.onComplete:
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)

    def fold[B](fail: Exception => B, succ: A => B)(using Executor): Fu[B] =
      fua map succ recover { case e: Exception => fail(e) }

    def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B])(using Executor): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception => fail(e) }

    def logFailure(logger: => lila.log.Logger, msg: Throwable => String)(using Executor): Fu[A] =
      addFailureEffect: e =>
        logger.warn(msg(e), e)

    def logFailure(logger: => lila.log.Logger)(using Executor): Fu[A] = logFailure(logger, _.toString)

    def addFailureEffect(effect: Throwable => Unit)(using Executor) =
      fua.failed.foreach: (e: Throwable) =>
        effect(e)
      fua

    def addEffect(effect: A => Unit)(using Executor): Fu[A] =
      fua foreach effect
      fua

    def addEffects(fail: Exception => Unit, succ: A => Unit)(using Executor): Fu[A] =
      fua.onComplete:
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      fua

    def addEffects(f: Try[A] => Unit)(using Executor): Fu[A] =
      fua onComplete f
      fua

    def addEffectAnyway(inAnyCase: => Unit)(using Executor): Fu[A] =
      fua.onComplete: _ =>
        inAnyCase
      fua

    def mapFailure(f: Exception => Exception)(using Executor): Fu[A] =
      fua.recoverWith:
        case cause: Exception => fufail(f(cause))

    def prefixFailure(p: => String)(using Executor): Fu[A] =
      mapFailure: e =>
        LilaException(s"$p ${e.getMessage}")

    def thenPp(using Executor): Fu[A] =
      effectFold(
        e => pprint.pprintln("[failure] " + e),
        a => pprint.pprintln("[success] " + a)
      )
      fua

    def thenPp(msg: String)(using Executor): Fu[A] =
      effectFold(
        e => pprint.pprintln(s"[$msg] [failure] $e"),
        a => pprint.pprintln(s"[$msg] [success] $a")
      )
      fua

    def await(duration: FiniteDuration, name: String): A =
      Chronometer.syncMon(_.blocking.time(name)) {
        try Await.result(fua, duration)
        catch
          case e: Exception =>
            lila.mon.blocking.timeout(name).increment()
            throw e
      }

    def awaitOrElse(duration: FiniteDuration, name: String, default: => A): A =
      try await(duration, name)
      catch case _: Exception => default

    def withTimeout(
        duration: FiniteDuration,
        error: => String
    )(using Executor, Scheduler): Fu[A] =
      withTimeoutError(duration, LilaTimeout(s"$error timeout after $duration"))

    def withTimeoutError(
        duration: FiniteDuration,
        error: => Exception with util.control.NoStackTrace
    )(using Executor)(using scheduler: Scheduler): Fu[A] =
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, scheduler)(Future failed error)
      )

    def withTimeoutDefault(
        duration: FiniteDuration,
        default: => A
    )(using Executor)(using scheduler: Scheduler): Fu[A] =
      Future firstCompletedOf Seq(
        fua,
        akka.pattern.after(duration, scheduler)(Future(default))
      )

    def delay(duration: FiniteDuration)(using Executor, Scheduler) =
      lila.common.LilaFuture.delay(duration)(fua)

    def chronometer    = Chronometer(fua)
    def chronometerTry = Chronometer.lapTry(fua)

    def mon(path: lila.mon.TimerPath): Fu[A]              = chronometer.mon(path).result
    def monTry(path: Try[A] => lila.mon.TimerPath): Fu[A] = chronometerTry.mon(r => path(r)(lila.mon)).result
    def monSuccess(path: lila.mon.type => Boolean => kamon.metric.Timer): Fu[A] =
      chronometerTry
        .mon: r =>
          path(lila.mon)(r.isSuccess)
        .result
    def monValue(path: A => lila.mon.TimerPath): Fu[A] = chronometer.monValue(path).result

    def logTime(name: String): Fu[A]                               = chronometer pp name
    def logTimeIfGt(name: String, duration: FiniteDuration): Fu[A] = chronometer.ppIfGt(name, duration)

    def recoverDefault(using Executor)(using z: Zero[A]): Fu[A] = recoverDefault(z.zero)

    def recoverDefault(using Executor)(default: => A): Fu[A] =
      fua.recover:
        case _: LilaException                         => default
        case _: java.util.concurrent.TimeoutException => default
        case e: Exception =>
          lila.log("common").warn("Future.recoverDefault", e)
          default

  extension (fua: Fu[Boolean])

    def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if _ then fub else fuFalse }(EC.parasitic)

    def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if _ then fuTrue else fub }(EC.parasitic)

    def flatMapz[B](fub: => Fu[B])(using zero: Zero[B]): Fu[B] =
      fua.flatMap { if _ then fub else fuccess(zero.zero) }(EC.parasitic)
    def mapz[B](fb: => B)(using zero: Zero[B]): Fu[B] =
      fua.map { if _ then fb else zero.zero }(EC.parasitic)

    inline def unary_! = fua.map { !_ }(EC.parasitic)

  extension [A](fua: Fu[Option[A]])

    def orFail(msg: => String)(using Executor): Fu[A] =
      fua.flatMap:
        _.fold[Fu[A]](fufail(msg))(fuccess)

    def orFailWith(err: => Exception)(using Executor): Fu[A] =
      fua.flatMap:
        _.fold[Fu[A]](fufail(err))(fuccess)

    def orElse(other: => Fu[Option[A]])(using Executor): Fu[Option[A]] =
      fua.flatMap:
        _.fold(other): x =>
          fuccess(Some(x))

    def getOrElse(other: => Fu[A])(using Executor): Fu[A] = fua flatMap { _.fold(other)(fuccess) }
    @targetName("orZeroFu")
    def orZeroFu(using z: Zero[A]): Fu[A] = fua.map(_ getOrElse z.zero)(EC.parasitic)

    def map2[B](f: A => B)(using Executor): Fu[Option[B]] = fua.map(_ map f)
    def dmap2[B](f: A => B): Fu[Option[B]]                = fua.map(_ map f)(EC.parasitic)

    def getIfPresent: Option[A] =
      fua.value match
        case Some(scala.util.Success(v)) => v
        case _                           => None

    def mapz[B: Zero](fb: A => B)(using Executor): Fu[B]          = fua.map { _ so fb }
    def flatMapz[B: Zero](fub: A => Fu[B])(using Executor): Fu[B] = fua.flatMap { _ so fub }
