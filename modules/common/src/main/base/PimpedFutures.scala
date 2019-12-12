package lila.base

import LilaTypes._
import ornicar.scalalib.Zero
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ Future, ExecutionContext }

/*
 * When calling map, foreach, flatMap, etc.. on a future,
 * the asynchronous callback is queued in the thread pool (execution context)
 * and ran asynchronously when a thread is available.
 * By specifying a DirectExecutionContext, we make the callback be called
 * immediately, synchronously, in the same thread that just completed
 * the future. This skips a trip in the thread pool and increases performance.
 * Only use when the callback is trivial.
 * E.g. futureString.map(_ + "!")(DirectExecutionContext)
 * or   futureString.dmap(_ + "!") // shortcut
 */
object DirectExecutionContext extends ExecutionContext {
  override def execute(command: Runnable): Unit = command.run()
  override def reportFailure(cause: Throwable): Unit =
    throw new IllegalStateException("lila DirectExecutionContext failure", cause)
}

final class PimpedFuture[A](private val fua: Fu[A]) extends AnyVal {
  private type Fu[A] = Future[A]

  // see DirectExecutionContext
  @inline def dmap[B](f: A => B): Fu[B] = fua.map(f)(DirectExecutionContext)
  @inline def dforeach[B](f: A => Unit): Unit = fua.foreach(f)(DirectExecutionContext)

  def >>-(sideEffect: => Unit): Fu[A] = fua andThen {
    case _ => sideEffect
  }

  def >>[B](fub: => Fu[B]): Fu[B] = fua flatMap { _ => fub }

  @inline def void: Fu[Unit] = dmap { _ => () }

  @inline def inject[B](b: => B): Fu[B] = dmap { _ => b }

  def injectAnyway[B](b: => B): Fu[B] = fold(_ => b, _ => b)

  def effectFold(fail: Exception => Unit, succ: A => Unit): Unit = {
    fua onComplete {
      case scala.util.Failure(e: Exception) => fail(e)
      case scala.util.Failure(e) => throw e // Throwables
      case scala.util.Success(e) => succ(e)
    }
  }

  def fold[B](fail: Exception => B, succ: A => B): Fu[B] =
    fua map succ recover { case e: Exception => fail(e) }

  def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B]): Fu[B] =
    fua flatMap succ recoverWith { case e: Exception => fail(e) }

  def logFailure(logger: => lila.log.Logger, msg: Exception => String): Fu[A] =
    addFailureEffect { e => logger.warn(msg(e), e) }
  def logFailure(logger: => lila.log.Logger): Fu[A] = logFailure(logger, _.toString)

  def addFailureEffect(effect: Exception => Unit) = {
    fua onFailure {
      case e: Exception => effect(e)
    }
    fua
  }

  def addEffect(effect: A => Unit): Fu[A] = {
    fua foreach effect
    fua
  }

  def addEffects(fail: Exception => Unit, succ: A => Unit): Fu[A] = {
    fua onComplete {
      case scala.util.Failure(e: Exception) => fail(e)
      case scala.util.Failure(e) => throw e // Throwables
      case scala.util.Success(e) => succ(e)
    }
    fua
  }

  def addEffectAnyway(inAnyCase: => Unit): Fu[A] = {
    fua onComplete { _ =>
      inAnyCase
    }
    fua
  }

  def mapFailure(f: Exception => Exception) = fua recoverWith {
    case cause: Exception => fufail(f(cause))
  }

  def prefixFailure(p: => String) = mapFailure { e =>
    LilaException(s"$p ${e.getMessage}")
  }

  def thenPp: Fu[A] = {
    effectFold(
      e => println("[failure] " + e),
      a => println("[success] " + a)
    )
    fua
  }

  def thenPp(msg: String): Fu[A] = {
    effectFold(
      e => println(s"[$msg] [failure] $e"),
      a => println(s"[$msg] [success] $a")
    )
    fua
  }

  def await(duration: FiniteDuration): A =
    scala.concurrent.Await.result(fua, duration)

  def awaitOrElse(duration: FiniteDuration, default: => A): A = try {
    scala.concurrent.Await.result(fua, duration)
  } catch {
    case _: Exception => default
  }

  def awaitSeconds(seconds: Int): A =
    await(seconds.seconds)

  def withTimeout(duration: FiniteDuration)(implicit system: akka.actor.ActorSystem): Fu[A] =
    withTimeout(duration, LilaException(s"Future timed out after $duration"))

  def withTimeout(duration: FiniteDuration, error: => Throwable)(implicit system: akka.actor.ActorSystem): Fu[A] = {
    Future firstCompletedOf Seq(
      fua,
      akka.pattern.after(duration, system.scheduler)(Future failed error)
    )
  }

  def withTimeoutDefault(duration: FiniteDuration, default: => A)(implicit system: akka.actor.ActorSystem): Fu[A] = {
    Future firstCompletedOf Seq(
      fua,
      akka.pattern.after(duration, system.scheduler)(Future(default))
    )
  }

  def delay(duration: FiniteDuration)(implicit system: akka.actor.ActorSystem) =
    lila.common.Future.delay(duration)(fua)

  def chronometer = lila.common.Chronometer(fua)

  def mon(path: lila.mon.RecPath) = chronometer.mon(path).result
  def logTime(name: String) = chronometer pp name
  def logTimeIfGt(name: String, duration: FiniteDuration) = chronometer.ppIfGt(name, duration)

  def nevermind(implicit z: Zero[A]): Fu[A] = fua recover {
    case e: LilaException => z.zero
    case e: java.util.concurrent.TimeoutException => z.zero
    case e: Exception =>
      lila.log("common").warn("Future.nevermind", e)
      z.zero
  }
}

final class PimpedFutureBoolean(private val fua: Fu[Boolean]) extends AnyVal {

  def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
    fua.flatMap { if (_) fub else fuFalse }(DirectExecutionContext)

  def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
    fua.flatMap { if (_) fuTrue else fub }(DirectExecutionContext)

  @inline def unary_! = fua.map { !_ }(DirectExecutionContext)
}

final class PimpedFutureOption[A](private val fua: Fu[Option[A]]) extends AnyVal {

  def flatten(msg: => String): Fu[A] = fua flatMap {
    _.fold[Fu[A]](fufail(msg))(fuccess(_))
  }

  def flattenWith(err: => Exception): Fu[A] = fua flatMap {
    _.fold[Fu[A]](fufail(err))(fuccess(_))
  }

  def orElse(other: => Fu[Option[A]]): Fu[Option[A]] = fua flatMap {
    _.fold(other) { x => fuccess(Some(x)) }
  }

  def getOrElse(other: => Fu[A]): Fu[A] = fua flatMap { _.fold(other)(fuccess) }
}

final class PimpedFutureValid[A](private val fua: Fu[Valid[A]]) extends AnyVal {

  def flatten: Fu[A] = fua.flatMap {
    _.fold[Fu[A]](fufail(_), fuccess(_))
  }(DirectExecutionContext)
}

final class PimpedTraversableFuture[A, M[X] <: TraversableOnce[X]](private val t: M[Fu[A]]) extends AnyVal {
  import scala.collection.generic.CanBuildFrom

  def sequenceFu(implicit cbf: CanBuildFrom[M[Fu[A]], A, M[A]]): Fu[M[A]] =
    Future.sequence(t)
}
