package lila.common

object Future {

  def lazyFold[T, R](futures: Stream[Fu[T]])(zero: R)(op: (R, T) => R): Fu[R] =
    Stream.cons.unapply(futures).fold(fuccess(zero)) {
      case (future, rest) => future flatMap { f =>
        lazyFold(rest)(op(zero, f))(op)
      }
    }
}
