package lila.common

import alleycats.Zero

trait LilaTest extends munit.FunSuite with EitherAssertions:

  def assertMatch[A](a: A)(f: PartialFunction[A, Boolean]) =
    assert(f.lift(a) | false, s"$a does not match expectations")

  def assertCloseTo[T](a: T, b: T, delta: Double)(using n: Numeric[T]) =
    assert(Maths.isCloseTo(a, b, delta), s"$a is not close to $b by $delta")

  extension [A](a: A)
    def matchZero[B: Zero](f: PartialFunction[A, B]): B =
      f.lift(a) | Zero[B].zero

trait EitherAssertions extends munit.Assertions:

  extension [E, A](v: Either[E, A])
    def assertRight(f: A => Any): Any = v match
      case Right(r) => f(r)
      case Left(e)  => fail(s"Expected Right but received $v")
