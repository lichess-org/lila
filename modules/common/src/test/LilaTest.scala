package lila.common

import alleycats.Zero

trait LilaTest extends munit.FunSuite with EitherAssertions:

  def assertMatch[A](a: A)(f: PartialFunction[A, Boolean])(using munit.Location) =
    assert(f.lift(a) | false, s"$a does not match expectations")

  def assertCloseTo[T](a: T, b: T, delta: Double)(using n: Numeric[T])(using munit.Location) =
    assert(scalalib.Maths.isCloseTo(a, b, delta), s"$a is not close to $b by $delta")

  extension [A](a: A)
    def matchZero[B: Zero](f: PartialFunction[A, B])(using munit.Location): B =
      f.lift(a) | Zero[B].zero

trait EitherAssertions extends munit.Assertions:

  extension [E, A](v: Either[E, A])
    def assertRight(f: A => Any)(using munit.Location): Any = v match
      case Right(r) => f(r)
      case Left(e)  => fail(s"Expected Right but received $v")
