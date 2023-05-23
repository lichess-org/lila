package lila.common

import akka.actor.Scheduler
import scala.collection.BuildFrom
import scala.concurrent.{ Future as ScalaFu, Promise }
import scala.concurrent.duration.FiniteDuration
import lila.Lila.Fu

object LilaFuture:

  def fold[T, R](list: List[T])(acc: R)(op: (R, T) => Fu[R])(using Executor): Fu[R] =
    list match
      case head :: rest =>
        op(acc, head) flatMap { res =>
          fold(rest)(res)(op)
        }
      case Nil => fuccess(acc)

  def lazyFold[T, R](futures: LazyList[Fu[T]])(acc: R)(op: (R, T) => R)(using Executor): Fu[R] =
    LazyList.cons.unapply(futures).fold(fuccess(acc)) { (future, rest) =>
      future flatMap { f =>
        lazyFold(rest)(op(acc, f))(op)
      }
    }

  def filter[A](list: List[A])(f: A => Fu[Boolean])(using Executor): Fu[List[A]] =
    ScalaFu
      .sequence {
        list.map { element =>
          f(element) dmap (_ option element)
        }
      }
      .dmap(_.flatten)

  def filterNot[A](
      list: List[A]
  )(f: A => Fu[Boolean])(using Executor): Fu[List[A]] =
    filter(list)(a => !f(a))

  def linear[A, B, M[B] <: Iterable[B]](
      in: M[A]
  )(f: A => Fu[B])(using cbf: BuildFrom[M[A], B, M[B]], ec: Executor): Fu[M[B]] =
    in.foldLeft(fuccess(cbf.newBuilder(in))) { (fr, a) =>
      fr flatMap { r =>
        f(a).dmap(r += _)
      }
    }.dmap(_.result())

  def applySequentially[A](
      list: List[A]
  )(f: A => Funit)(using Executor): Funit =
    list match
      case h :: t => f(h) >> applySequentially(t)(f)
      case Nil    => funit

  def find[A](
      list: List[A]
  )(f: A => Fu[Boolean])(using Executor): Fu[Option[A]] =
    list match
      case Nil => fuccess(none)
      case h :: t =>
        f(h).flatMap {
          if _ then fuccess(h.some)
          else find(t)(f)
        }

  def exists[A](list: List[A])(pred: A => Fu[Boolean])(using Executor): Fu[Boolean] =
    find(list)(pred).dmap(_.isDefined)

  def delay[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: lila.Lila.Executor, scheduler: Scheduler): Fu[A] =
    if (duration == 0.millis) run
    else akka.pattern.after(duration, scheduler)(run)

  def sleep(duration: FiniteDuration)(using ec: Executor, scheduler: Scheduler): Funit =
    val p = Promise[Unit]()
    scheduler.scheduleOnce(duration)(p success {})
    p.future

  def makeItLast[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: Executor, scheduler: Scheduler): Fu[A] =
    if (duration == 0.millis) run
    else run zip akka.pattern.after(duration, scheduler)(funit) dmap (_._1)

  def retry[T](op: () => Fu[T], delay: FiniteDuration, retries: Int, logger: Option[lila.log.Logger])(using
      ec: Executor,
      scheduler: Scheduler
  ): Fu[T] =
    op() recoverWith {
      case e if retries > 0 =>
        logger foreach { _.info(s"$retries retries - ${e.getMessage}") }
        akka.pattern.after(delay, scheduler)(retry(op, delay, retries - 1, logger))
    }
