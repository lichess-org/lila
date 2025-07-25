package lila.core.lilaism

import alleycats.Zero
import com.typesafe.config.Config
import scalalib.future.FutureAfter
import java.util.concurrent.TimeUnit
import scala.collection.BuildFrom
import scala.concurrent.{ ExecutionContext as EC, Future }
import scala.util.Try

trait LilaLibraryExtensions extends CoreExports:

  export scalalib.future.extensions.*
  export scalalib.future.given_Zero_Future

  given [A]: Zero[Update[A]] with
    def zero = identity[A]

  def fuccess[A](a: A): Fu[A] = Future.successful(a)
  def fufail[X](t: Throwable): Fu[X] = Future.failed(t)
  def fufail[X](s: String): Fu[X] = fufail(LilaException(s))
  val funit = Future.unit
  val fuTrue = fuccess(true)
  val fuFalse = fuccess(false)

  /* library-agnostic way to run a future after a delay */
  given (using sched: Scheduler, ec: Executor): FutureAfter =
    [A] => (duration: FiniteDuration) => (fua: () => Future[A]) => akka.pattern.after(duration, sched)(fua())

  extension [A](self: Option[A])

    def toTryWith(err: => Exception): Try[A] =
      self.fold(scala.util.Failure(err))(scala.util.Success.apply)

    def toTry(err: => String): Try[A] = toTryWith(LilaException(err))

    def err(message: => String): A = self.getOrElse(sys.error(message))

  extension (self: Boolean)
    def not: Boolean = !self
    // move to scalalib? generalize Future away?
    def soFu[B](f: => Future[B]): Future[Option[B]] =
      if self then f.map(Some(_))(using scala.concurrent.ExecutionContext.parasitic)
      else Future.successful(None)

  extension (config: Config)
    def millis(name: String): Int = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
    def seconds(name: String): Int = config.getDuration(name, TimeUnit.SECONDS).toInt
    def duration(name: String): FiniteDuration = millis(name).millis

  extension [A, B](v: Either[A, B])
    def toFuture: Fu[B] = v match
      case Right(res) => fuccess(res)
      case Left(err) =>
        err match
          case e: Exception => Future.failed(e)
          case _ => fufail(err.toString)

  extension [A, B](v: (A, B)) def map2[C](f: B => C): (A, C) = (v._1, f(v._2))

  extension (d: FiniteDuration)
    def toCentis = chess.Centis(d)
    def abs = if d.length < 0 then -d else d

  extension [A](list: List[A])
    def sortLike[B](other: Seq[B], f: A => B): List[A] =
      list.sortWith: (x, y) =>
        other.indexOf(f(x)) < other.indexOf(f(y))
    def tailOption: Option[List[A]] = list match
      case Nil => None
      case _ :: rest => Some(rest)
    def tailSafe: List[A] = tailOption.getOrElse(Nil)
    def indexOption(a: A) = Option(list.indexOf(a)).filter(0 <= _)
    def previous(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i - 1))
    def next(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i + 1))
    def sortedReverse(using ord: Ordering[A]): List[A] = list.sorted(using ord.reverse)
    def sortByReverse[B](f: A => B)(using ord: Ordering[B]): List[A] =
      list.sortBy(f)(using ord.reverse)

    def sequentially[B](f: A => Fu[B])(using Executor): Fu[List[B]] =
      list
        .foldLeft(fuccess(List.empty[B])): (acc, a) =>
          acc.flatMap: bs =>
            f(a).map(_ :: bs)
        .map(_.reverse)
    def sequentiallyVoid(f: A => Fu[?])(using Executor): Funit =
      list
        .foldLeft(funit): (acc, a) =>
          acc.flatMap: _ =>
            f(a).void

  extension [A, M[A] <: IterableOnce[A]](list: M[A])
    def parallel[B](f: A => Fu[B])(using Executor, BuildFrom[M[A], B, M[B]]): Fu[M[B]] =
      Future.traverse(list)(f)

    def parallelVoid[B](f: A => Fu[B])(using Executor): Fu[Unit] =
      list.iterator
        .foldLeft(fuccess(()))((fr, a) => fr.zipWith(f(a))((_, _) => ()))

  extension [A, M[A] <: IterableOnce[A]](list: M[Fu[A]])

    def parallel(using Executor, BuildFrom[M[Future[A]], A, M[A]]): Fu[M[A]] =
      Future.sequence(list)

    def parallelVoid(using Executor): Fu[Unit] =
      list.iterator
        .foldLeft(fuccess(()))((fr, fa) => fr.zipWith(fa)((_, _) => ()))

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
      fua.flatMap { if _ then fub else fuFalse }(using EC.parasitic)

    infix def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
      fua.flatMap { if _ then fuTrue else fub }(using EC.parasitic)

    infix def flatMapz[B](fub: => Fu[B])(using zero: Zero[B]): Fu[B] =
      fua.flatMap { if _ then fub else fuccess(zero.zero) }(using EC.parasitic)
    def mapz[B](fb: => B)(using zero: Zero[B]): Fu[B] =
      fua.map { if _ then fb else zero.zero }(using EC.parasitic)

    // inline def unary_! = fua.map { !_ }(EC.parasitic)
    inline def not = fua.map { !_ }(using EC.parasitic)
