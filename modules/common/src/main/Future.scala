package lila.common

import scala.concurrent.duration._

object Future {

  def fold[T, R](list: List[T])(zero: R)(op: (R, T) => Fu[R]): Fu[R] =
    list match {
      case head :: rest => op(zero, head) flatMap { res =>
        fold(rest)(res)(op)
      }
      case Nil => fuccess(zero)
    }

  def lazyFold[T, R](futures: Stream[Fu[T]])(zero: R)(op: (R, T) => R): Fu[R] =
    Stream.cons.unapply(futures).fold(fuccess(zero)) {
      case (future, rest) => future flatMap { f =>
        lazyFold(rest)(op(zero, f))(op)
      }
    }

  def filter[A](list: List[A])(f: A => Fu[Boolean]): Fu[List[A]] = list.map {
    element => f(element) dmap (_ option element)
  }.sequenceFu.dmap(_.flatten)

  def filterNot[A](list: List[A])(f: A => Fu[Boolean]): Fu[List[A]] =
    filter(list)(a => !f(a))

  def traverseSequentially[A, B](list: List[A])(f: A => Fu[B]): Fu[List[B]] =
    list match {
      case h :: t => f(h).flatMap { r =>
        traverseSequentially(t)(f) dmap (r +: _)
      }
      case Nil => fuccess(Nil)
    }

  def applySequentially[A](list: List[A])(f: A => Funit): Funit =
    list match {
      case h :: t => f(h) >> applySequentially(t)(f)
      case Nil => funit
    }

  def find[A](list: List[A])(f: A => Fu[Boolean]): Fu[Option[A]] = list match {
    case Nil => fuccess(none)
    case h :: t => f(h).flatMap {
      case true => fuccess(h.some)
      case false => find(t)(f)
    }
  }

  def exists[A](list: List[A])(pred: A => Fu[Boolean]): Fu[Boolean] = find(list)(pred).map(_.isDefined)

  def delay[A](duration: FiniteDuration)(run: => Fu[A])(implicit system: akka.actor.ActorSystem): Fu[A] =
    if (duration == 0.millis) run
    else akka.pattern.after(duration, system.scheduler)(run)

  def makeItLast[A](duration: FiniteDuration)(run: => Fu[A])(implicit system: akka.actor.ActorSystem): Fu[A] =
    if (duration == 0.millis) run
    else run zip akka.pattern.after(duration, system.scheduler)(funit) dmap (_._1)

  def retry[T](op: () => Fu[T], delay: FiniteDuration, retries: Int, logger: Option[lila.log.Logger])(implicit system: akka.actor.ActorSystem): Fu[T] =
    op() recoverWith {
      case e if retries > 0 =>
        logger foreach { _.info(s"$retries retries - ${e.getMessage}") }
        akka.pattern.after(delay, system.scheduler)(retry(op, delay, retries - 1, logger))
    }
}
