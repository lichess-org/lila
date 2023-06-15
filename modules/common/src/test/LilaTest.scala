package lila.common

import alleycats.Zero

trait LilaTest extends munit.FunSuite {

  def assertMatch[A](a: A)(f: PartialFunction[A, Boolean]) =
    assert(f.lift(a) | false, s"$a does not match expectations")

  def assertCloseTo[T](a: T, b: T, delta: Double)(using n: Numeric[T]) =
    assert(Maths.isCloseTo(a, b, delta), s"$a is not close to $b by $delta")

  extension [A](a: A)
    def matchZero[B: Zero](f: PartialFunction[A, B]): B =
      f.lift(a) | Zero[B].zero
}
