package lila.base

import akka.actor.ActorSystem
import ornicar.scalalib.Zero
import scala.collection.BuildFrom
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext => EC, Future, Await }
import scala.util.Try

import lila.common.Chronometer
import LilaTypes._

final class PimpedFuture[A](private val fua: Fu[A]) extends AnyVal {

  @inline def dmap[B](f: A => B): Fu[B]       = fua.map(f)(EC.parasitic)
  @inline def dforeach[B](f: A => Unit): Unit = fua.foreach(f)(EC.parasitic)

  def >>-(sideEffect: => Unit)(implicit ec: EC): Fu[A] =
    fua andThen { case _ =>
      sideEffect
    }

  def >>[B](fub: => Fu[B])(implicit ec: EC): Fu[B] =
    fua flatMap { _ =>
      fub
    }

  @inline def void: Fu[Unit] =
    dmap { _ =>
      ()
    }

  @inline def inject[B](b: => B): Fu[B] =
    dmap { _ =>
      b
    }

  def injectAnyway[B](b: => B)(implicit ec: EC): Fu[B] = fold(_ => b, _ => b)

  def effectFold(fail: Exception => Unit, succ: A => Unit)(implicit ec: EC): Unit = {
    fua onComplete {
      case scala.util.Failure(e: Exception) => fail(e)
      case scala.util.Failure(e)            => throw e // Throwables
      case scala.util.Success(e)            => succ(e)
    }
  }

  def fold[B](fail: Exception => B, succ: A => B)(implicit ec: EC): Fu[B] =
    fua map succ recover { case e: Exception => fail(e) }

  def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B])(implicit ec: EC): Fu[B] =
    fua flatMap succ recoverWith { case e: Exception => fail(e) }

  def logFailure(logger: => lila.log.Logger, msg: Throwable => String)(implicit ec: EC): Fu[A] =
    addFailureEffect { e =>
      logger.warn(msg(e), e)
    }
  def logFailure(logger: => lila.log.Logger)(implicit ec: EC): Fu[A] = logFailure(logger, _.toString)

  def addFailureEffect(effect: Throwable => Unit)(implicit ec: EC) = {
    fua.failed.foreach { e: Throwable =>
      effect(e)
    }
    fua
  }

  def addEffect(effect: A => Unit)(implicit ec: EC): Fu[A] = {
    fua foreach effect
    fua
  }

  def addEffects(fail: Exception => Unit, succ: A => Unit)(implicit ec: EC): Fu[A] = {
    fua onComplete {
      case scala.util.Failure(e: Exception) => fail(e)
      case scala.util.Failure(e)            => throw e // Throwables
      case scala.util.Success(e)            => succ(e)
    }
    fua
  }

  def addEffects(f: Try[A] => Unit)(implicit ec: EC): Fu[A] = {
    fua onComplete f
    fua
  }

  def addEffectAnyway(inAnyCase: => Unit)(implicit ec: EC): Fu[A] = {
    fua onComplete { _ =>
      inAnyCase
    }
    fua
  }

  def mapFailure(f: Exception => Exception)(implicit ec: EC) =
    fua recoverWith { case cause: Exception =>
      fufail(f(cause))
    }

  def prefixFailure(p: => String)(implicit ec: EC) =
    mapFailure { e =>
      LilaException(s"$p ${e.getMessage}")
    }

  def thenPp(implicit ec: EC): Fu[A] = {
    effectFold(
      e => println("[failure] " + e),
      a => println("[success] " + a)
    )
    fua
  }

  def thenPp(msg: String)(implicit ec: EC): Fu[A] = {
    effectFold(
      e => println(s"[$msg] [failure] $e"),
      a => println(s"[$msg] [success] $a")
    )
    fua
  }

  def await(duration: FiniteDuration, name: String): A =
    Chronometer.syncMon(_.blocking.time(name)) {
      Await.result(fua, duration)
    }

  def awaitOrElse(duration: FiniteDuration, name: String, default: => A): A =
    try {
      await(duration, name)
    } catch {
      case _: Exception => default
    }

  def withTimeout(duration: FiniteDuration)(implicit ec: EC, system: ActorSystem): Fu[A] =
    withTimeout(duration, LilaTimeout(s"Future timed out after $duration"))

  def withTimeout(
      duration: FiniteDuration,
      error: => Throwable
  )(implicit ec: EC, system: ActorSystem): Fu[A] = {
    Future firstCompletedOf Seq(
      fua,
      akka.pattern.after(duration, system.scheduler)(Future failed error)
    )
  }

  def withTimeoutDefault(
      duration: FiniteDuration,
      default: => A
  )(implicit ec: EC, system: ActorSystem): Fu[A] = {
    Future firstCompletedOf Seq(
      fua,
      akka.pattern.after(duration, system.scheduler)(Future(default))
    )
  }

  def delay(duration: FiniteDuration)(implicit ec: EC, system: ActorSystem) =
    lila.common.Future.delay(duration)(fua)

  def chronometer    = Chronometer(fua)
  def chronometerTry = Chronometer.lapTry(fua)

  def mon(path: lila.mon.TimerPath)              = chronometer.mon(path).result
  def monTry(path: Try[A] => lila.mon.TimerPath) = chronometerTry.mon(r => path(r)(lila.mon)).result
  def monSuccess(path: lila.mon.type => Boolean => kamon.metric.Timer) =
    chronometerTry.mon { r =>
      path(lila.mon)(r.isSuccess)
    }.result
  def monValue(path: A => lila.mon.TimerPath) = chronometer.monValue(path).result

  def logTime(name: String)                               = chronometer pp name
  def logTimeIfGt(name: String, duration: FiniteDuration) = chronometer.ppIfGt(name, duration)

  def nevermind(implicit z: Zero[A], ec: EC): Fu[A] = nevermind(z.zero)

  def nevermind(default: => A)(implicit ec: EC): Fu[A] =
    fua recover {
      case _: LilaException                         => default
      case _: java.util.concurrent.TimeoutException => default
      case e: Exception =>
        lila.log("common").warn("Future.nevermind", e)
        default
    }
}

final class PimpedFutureBoolean(private val fua: Fu[Boolean]) extends AnyVal {

  def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
    fua.flatMap { if (_) fub else fuFalse }(EC.parasitic)

  def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
    fua.flatMap { if (_) fuTrue else fub }(EC.parasitic)

  @inline def unary_! = fua.map { !_ }(EC.parasitic)
}

final class PimpedFutureOption[A](private val fua: Fu[Option[A]]) extends AnyVal {

  def orFail(msg: => String)(implicit ec: EC): Fu[A] =
    fua flatMap {
      _.fold[Fu[A]](fufail(msg))(fuccess)
    }

  def orFailWith(err: => Exception)(implicit ec: EC): Fu[A] =
    fua flatMap {
      _.fold[Fu[A]](fufail(err))(fuccess)
    }

  def orElse(other: => Fu[Option[A]])(implicit ec: EC): Fu[Option[A]] =
    fua flatMap {
      _.fold(other) { x =>
        fuccess(Some(x))
      }
    }

  def getOrElse(other: => Fu[A])(implicit ec: EC): Fu[A] = fua flatMap { _.fold(other)(fuccess) }

  def map2[B](f: A => B)(implicit ec: EC): Fu[Option[B]] = fua.map(_ map f)
  def dmap2[B](f: A => B): Fu[Option[B]]                 = fua.map(_ map f)(EC.parasitic)
}

// final class PimpedFutureValid[A](private val fua: Fu[Valid[A]]) extends AnyVal {

//   def flatten: Fu[A] = fua.flatMap {
//     _.fold[Fu[A]](fufail(_), fuccess(_))
//   }(EC.parasitic)
// }

final class PimpedIterableFuture[A, M[X] <: IterableOnce[X]](private val t: M[Fu[A]]) extends AnyVal {
  def sequenceFu(implicit bf: BuildFrom[M[Fu[A]], A, M[A]], ec: EC): Fu[M[A]] = Future.sequence(t)
}
