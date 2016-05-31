package lila.common

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
      case Nil => funit
    }
}
