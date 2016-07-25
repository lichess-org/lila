package lila.common

import scala.concurrent.duration._

object Future {

  def lazyFold[T, R](futures: Stream[Fu[T]])(zero: R)(op: (R, T) => R): Fu[R] =
    Stream.cons.unapply(futures).fold(fuccess(zero)) {
      case (future, rest) => future flatMap { f =>
        lazyFold(rest)(op(zero, f))(op)
      }
    }

  def filter[A](list: List[A])(f: A => Fu[Boolean]): Fu[List[A]] = {
    list.map {
      element => f(element) map (_ option element)
    }.sequenceFu.map(_.flatten)
  }

  def filterNot[A](list: List[A])(f: A => Fu[Boolean]): Fu[List[A]] = {
    list.map {
      element => !f(element) map (_ option element)
    }.sequenceFu.map(_.flatten)
  }

  def traverseSequentially[A, B](list: List[A])(f: A => Fu[B]): Fu[List[B]] =
    list match {
      case h :: t => f(h).flatMap { r =>
        traverseSequentially(t)(f) map (r +: _)
      }
      case Nil => fuccess(Nil)
    }

  def applySequentially[A](list: List[A])(f: A => Funit): Funit =
    list match {
      case h :: t => f(h) >> applySequentially(t)(f)
      case Nil    => funit
    }

  def find[A](list: List[A])(f: A => Fu[Boolean]): Fu[Option[A]] = list match {
    case Nil => fuccess(none)
    case h :: t => f(h).flatMap {
      case true  => fuccess(h.some)
      case false => find(t)(f)
    }
  }

  def delay[A](duration: FiniteDuration)(run: => Fu[A])(implicit system: akka.actor.ActorSystem): Fu[A] =
    akka.pattern.after(duration, system.scheduler)(run)
}
