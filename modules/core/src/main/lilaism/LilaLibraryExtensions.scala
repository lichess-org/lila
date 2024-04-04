package lila.core.lilaism

import alleycats.Zero
import com.typesafe.config.Config
import scalalib.extensions.*

import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.concurrent.{ ExecutionContext as EC }
import scala.util.Try
import scala.util.matching.Regex
import scalalib.future.FutureAfter

trait LilaLibraryExtensions extends LilaTypes:

  export scalalib.future.extensions.*
  export scalalib.future.given_Zero_Future

  /* library-agnostic way to run a future after a delay */
  given (using sched: Scheduler, ec: Executor): FutureAfter =
    [A] => (duration: FiniteDuration) => (fua: () => Future[A]) => akka.pattern.after(duration, sched)(fua())

  extension [A](self: Option[A])

    def toTryWith(err: => Exception): Try[A] =
      self.fold[Try[A]](scala.util.Failure(err))(scala.util.Success.apply)

    def toTry(err: => String): Try[A] = toTryWith(LilaException(err))

    def err(message: => String): A = self.getOrElse(sys.error(message))

  extension (self: Boolean)
    // move to scalalib? generalize Future away?
    def soFu[B](f: => Future[B]): Future[Option[B]] =
      if self then f.map(Some(_))(scala.concurrent.ExecutionContext.parasitic)
      else Future.successful(None)

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
      list.sortWith: (x, y) =>
        other.indexOf(f(x)) < other.indexOf(f(y))
    def tailOption: Option[List[A]] = list match
      case Nil       => None
      case _ :: rest => Some(rest)
    def tailSafe: List[A]         = tailOption.getOrElse(Nil)
    def indexOption(a: A)         = Option(list.indexOf(a)).filter(0 <= _)
    def previous(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i - 1))
    def next(a: A): Option[A]     = indexOption(a).flatMap(i => list.lift(i + 1))

  extension (self: Array[Byte]) def toBase64 = Base64.getEncoder.encodeToString(self)

  // run a collection of futures in parallel
  extension [A](list: List[Fu[A]]) def parallel(using Executor): Fu[List[A]]         = Future.sequence(list)
  extension [A](vec: Vector[Fu[A]]) def parallel(using Executor): Fu[Vector[A]]      = Future.sequence(vec)
  extension [A](set: Set[Fu[A]]) def parallel(using Executor): Fu[Set[A]]            = Future.sequence(set)
  extension [A](seq: Seq[Fu[A]]) def parallel(using Executor): Fu[Seq[A]]            = Future.sequence(seq)
  extension [A](iter: Iterable[Fu[A]]) def parallel(using Executor): Fu[Iterable[A]] = Future.sequence(iter)
  extension [A](iter: Iterator[Fu[A]]) def parallel(using Executor): Fu[Iterator[A]] = Future.sequence(iter)

  extension [A](fua: Fu[A])

    infix def >>[B](fub: => Fu[B])(using Executor): Fu[B] =
      fua.flatMap(_ => fub)

    def fold[B](fail: Exception => B, succ: A => B)(using Executor): Fu[B] =
      fua.map(succ).recover { case e: Exception => fail(e) }

    def logFailure(logger: => play.api.LoggerLike, msg: Throwable => String)(using Executor): Fu[A] =
      fua.addFailureEffect: e =>
        logger.warn(msg(e), e)

    def logFailure(logger: => play.api.LoggerLike)(using Executor): Fu[A] = logFailure(logger, _.toString)

    def mapFailure(f: Exception => Exception)(using Executor): Fu[A] =
      fua.recoverWith:
        case cause: Exception => fufail(f(cause))

    def prefixFailure(p: => String)(using Executor): Fu[A] =
      mapFailure: e =>
        LilaException(s"$p ${e.getMessage}")

    def thenPp(using Executor): Fu[A] =
      fua.addEffects(
        e => pprint.pprintln("[failure] " + e),
        a => pprint.pprintln("[success] " + a)
      )

    def thenPp(msg: String)(using Executor): Fu[A] =
      fua.addEffects(
        e => pprint.pprintln(s"[$msg] [failure] $e"),
        a => pprint.pprintln(s"[$msg] [success] $a")
      )

  extension (fua: Fu[Boolean])

    infix def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if _ then fub else fuFalse }(EC.parasitic)

    infix def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if _ then fuTrue else fub }(EC.parasitic)

    infix def flatMapz[B](fub: => Fu[B])(using zero: Zero[B]): Fu[B] =
      fua.flatMap { if _ then fub else fuccess(zero.zero) }(EC.parasitic)
    def mapz[B](fb: => B)(using zero: Zero[B]): Fu[B] =
      fua.map { if _ then fb else zero.zero }(EC.parasitic)

    // inline def unary_! = fua.map { !_ }(EC.parasitic)
    inline def not = fua.map { !_ }(EC.parasitic)
