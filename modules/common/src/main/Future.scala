package lila.common

import akka.actor.{ ActorSystem, Scheduler }
import scala.collection.BuildFrom
import scala.concurrent.duration.*
import scala.concurrent.{ ExecutionContext as EC, Future as ScalaFu, Promise }
import lila.Lila.Fu

object Future:

  def fold[T, R](
      list: List[T]
  )(zero: R)(op: (R, T) => Fu[R])(using ec: EC): Fu[R] =
    list match
      case head :: rest =>
        op(zero, head) flatMap { res =>
          fold(rest)(res)(op)
        }
      case Nil => fuccess(zero)

  def lazyFold[T, R](
      futures: LazyList[Fu[T]]
  )(zero: R)(op: (R, T) => R)(using ec: EC): Fu[R] =
    LazyList.cons.unapply(futures).fold(fuccess(zero)) { case (future, rest) =>
      future flatMap { f =>
        lazyFold(rest)(op(zero, f))(op)
      }
    }

  def filter[A](
      list: List[A]
  )(f: A => Fu[Boolean])(using ec: EC): Fu[List[A]] =
    ScalaFu
      .sequence {
        list.map { element =>
          f(element) dmap (_ option element)
        }
      }
      .dmap(_.flatten)

  def filterNot[A](
      list: List[A]
  )(f: A => Fu[Boolean])(using ec: EC): Fu[List[A]] =
    filter(list)(a => !f(a))

  def linear[A, B, M[B] <: Iterable[B]](
      in: M[A]
  )(f: A => Fu[B])(implicit cbf: BuildFrom[M[A], B, M[B]], ec: EC): Fu[M[B]] =
    in.foldLeft(fuccess(cbf.newBuilder(in))) { (fr, a) =>
      fr flatMap { r =>
        f(a).dmap(r += _)
      }
    }.dmap(_.result())

  def applySequentially[A](
      list: List[A]
  )(f: A => Funit)(using ec: EC): Funit =
    list match
      case h :: t => f(h) >> applySequentially(t)(f)
      case Nil    => funit

  def find[A](
      list: List[A]
  )(f: A => Fu[Boolean])(using ec: EC): Fu[Option[A]] =
    list match
      case Nil => fuccess(none)
      case h :: t =>
        f(h).flatMap {
          case true  => fuccess(h.some)
          case false => find(t)(f)
        }

  def exists[A](list: List[A])(pred: A => Fu[Boolean])(using
      ec: EC
  ): Fu[Boolean] = find(list)(pred).dmap(_.isDefined)

  def delay[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: EC, scheduler: Scheduler): Fu[A] =
    if (duration == 0.millis) run
    else akka.pattern.after(duration, scheduler)(run)

  def sleep(duration: FiniteDuration)(using ec: EC, scheduler: Scheduler): Funit =
    val p = Promise[Unit]()
    scheduler.scheduleOnce(duration)(p success {})
    p.future

  def makeItLast[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: EC, scheduler: Scheduler): Fu[A] =
    if (duration == 0.millis) run
    else run zip akka.pattern.after(duration, scheduler)(funit) dmap (_._1)

  def retry[T](op: () => Fu[T], delay: FiniteDuration, retries: Int, logger: Option[lila.log.Logger])(using
      ec: EC,
      scheduler: Scheduler
  ): Fu[T] =
    op() recoverWith {
      case e if retries > 0 =>
        logger foreach { _.info(s"$retries retries - ${e.getMessage}") }
        akka.pattern.after(delay, scheduler)(retry(op, delay, retries - 1, logger))
    }
