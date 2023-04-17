package lila.common

import alleycats.Zero

trait LilaTest extends munit.FunSuite {

  def assertMatch[A](a: A)(f: PartialFunction[A, Boolean]) =
    assert(f.lift(a) | false, s"$a does not match expectations")

  extension [A](a: A)
    def matchZero[B: Zero](f: PartialFunction[A, B]): B =
      f.lift(a) | Zero[B].zero
}
