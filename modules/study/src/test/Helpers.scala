package lila.study

import chess.format.pgn.{ PgnStr, Tags }
import monocle.syntax.all.*
import alleycats.Zero

import lila.tree.Node.{ Comment, Comments }
import lila.tree.{ Branch, Branches, Metas, NewBranch, NewRoot, NewTree, Node, Root }

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

  def rootToPgn(root: NewRoot): PgnStr = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.withoutOrientation)
    .render

  extension (root: Root)
    def toNewRoot: NewRoot = NewRoot(root)

    def withoutClockTrust: Root =
      toNewRoot.withoutClockTrust.toRoot

    def debug = root.ppAs(rootToPgn)

  extension (newBranch: NewBranch)
    def toBranch(children: Option[NewTree]): Branch = Branch(
      newBranch.ply,
      newBranch.move,
      newBranch.fen,
      newBranch.eval,
      newBranch.shapes,
      newBranch.comments,
      newBranch.gamebook,
      newBranch.glyphs,
      children.fold(Branches.empty)(_.toBranches),
      newBranch.opening,
      newBranch.comp,
      newBranch.clock,
      newBranch.crazyData,
      newBranch.forceVariation
    )

  extension (newTree: NewTree)
    // We lost variations here
    // newTree.toBranch == newTree.withoutVariations.toBranch
    def toBranch: Branch = newTree.value.toBranch(newTree.child)

    def toBranches: Branches =
      val variations = newTree.variations.map(_.toNode.toBranch)
      Branches(newTree.value.toBranch(newTree.child) :: variations)

  extension (root: NewRoot)
    def toRoot =
      Root(
        root.ply,
        root.fen,
        root.eval,
        root.shapes,
        root.comments,
        root.gamebook,
        root.glyphs,
        root.tree.fold(Branches.empty)(_.toBranches),
        root.opening,
        root.clock,
        root.crazyData
      )

    def cleanup: NewRoot =
      root
        .focus(_.tree.some)
        .modify(_.map(_.cleanup))
        .focus(_.metas.comments)
        .modify(_.cleanup)

    def withoutClockTrust: NewRoot =
      root
        .focus(_.metas.clock.some.trust)
        .replace(none)
        .focus(_.tree.some)
        .modify(_.map(_.focus(_.metas.clock.some.trust).replace(none)))

    def debug = root.ppAs(rootToPgn)

  extension (comments: Comments)
    def cleanup: Comments =
      Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

  extension (node: NewBranch)
    def cleanup: NewBranch =
      node
        .focus(_.metas.clock)
        .replace(none)
        .focus(_.metas.comments)
        .modify(_.cleanup)
