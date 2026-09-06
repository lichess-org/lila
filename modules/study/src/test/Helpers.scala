package lila.study

import chess.format.pgn.{ PgnStr, Tags }
import monocle.syntax.all.*
import alleycats.Zero

import lila.tree.Root

trait LilaTest extends munit.FunSuite with EitherAssertions:

  def assertMatch[A](a: A)(f: PartialFunction[A, Boolean])(using munit.Location) =
    assert(f.lift(a) | false, s"$a does not match expectations")

  def assertCloseTo[T](a: T, b: T, delta: Double)(using n: Numeric[T])(using munit.Location) =
    assert(scalalib.Maths.isCloseTo(a, b, delta), s"$a is not close to $b by $delta")

  extension [A](a: A)
    def matchZero[B: Zero](f: PartialFunction[A, B]): B =
      f.lift(a) | Zero[B].zero

trait EitherAssertions extends munit.Assertions:

  extension [E, A](v: Either[E, A])
    def assertRight(f: A => Any)(using munit.Location): Any = v match
      case Right(r) => f(r)
      case Left(_) => fail(s"Expected Right but received $v")

object Helpers:

  def rootToPgn(root: Root): PgnStr = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.withoutOrientation)
    .render

  extension (root: Root)
    def withoutClockTrust: Root =
      root
        .focus(_.clock.some.trust)
        .replace(none)
        .focus(_.children)
        .modify(_.updateAllWith(_.focus(_.clock.some.trust).replace(none)))

    def debug = root.ppAs(rootToPgn)
