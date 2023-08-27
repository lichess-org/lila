package lila.study

import chess.{ Centis, ErrorStr, Node as PgnNode, Situation }
import chess.format.pgn.{ Glyphs, ParsedPgn, San, Tags, PgnStr, PgnNodeData, Comment as ChessComment }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.MoveOrDrop.*

import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.syntax.all.*
import StudyArbitraries.{ *, given }
import chess.CoreArbitraries.given
import org.scalacheck.Prop.{ forAll, propBoolean }
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }
import chess.format.pgn.Glyph

class NewTreeTest extends munit.ScalaCheckSuite:

  import PgnImport.*
  import lila.tree.NewTree.*
  import Helpers.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  test("tree <-> newTree conversion"):
    PgnFixtures.all.foreach: pgn =>
      val x       = PgnImport(pgn, Nil).toOption.get
      val newRoot = x.root.toNewRoot
      assertEquals(newRoot.toRoot, x.root)

  test("PgnImport works"):
    PgnFixtures.all.foreach: pgn =>
      val x = PgnImport(pgn, Nil).toOption.get
      val y = NewPgnImport(pgn, Nil).toOption.get
      assertEquals(y.end, x.end)
      assertEquals(y.variant, x.variant)
      assertEquals(y.tags, x.tags)
      val oldRoot = x.root.toNewRoot.cleanup
      assertEquals(y.root.cleanup, oldRoot)

  test("conversion check"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      val newRoot = oldRoot.toNewRoot
      assertEquals(root, newRoot)

  test("path exists"):
    forAll: (rp: RootWithPath) =>
      val (root, path) = rp
      val oldRoot      = root.toRoot
      oldRoot.pathExists(path) == root.pathExists(path)

  test("setShapesAt"):
    forAll: (rp: RootWithPath, shapes: Shapes) =>
      val (root, path) = rp
      val oldRoot      = root.toRoot
      oldRoot.setShapesAt(shapes, path).map(_.toNewRoot) == root.modifyAt(path, _.copy(shapes = shapes))

  test("toggleGlyphAt"):
    forAll: (rp: RootWithPath, glyph: Glyph) =>
      val (root, path) = rp
      val oldRoot      = root.toRoot
      oldRoot.toggleGlyphAt(glyph, path).map(_.toNewRoot) == root.modifyAt(path, _.toggleGlyph(glyph))

  test("setClockAt"):
    forAll: (rp: RootWithPath, clock: Option[Centis]) =>
      val (root, path) = rp
      val oldRoot      = root.toRoot
      oldRoot.setClockAt(clock, path).map(_.toNewRoot) == root.modifyAt(path, _.copy(clock = clock))

  test("updateMainlineLast"):
    forAll: (root: NewRoot, c: Option[Centis]) =>
      val oldRoot = root.toRoot
      oldRoot.updateMainlineLast(_.copy(clock = c)).toNewRoot == root.updateMainlineLast(_.copy(clock = c))

  test("clearVariations"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.clearVariations.toNewRoot == root.clearVariations

  test("mainline"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.mainline.map(NewTree.fromBranch(_)) == root.mainlineValues

  override def scalaCheckInitialSeed = "CgQHx_FFpjlSeG9q8tbxLV7PE1y0JGJf9AOdzECxTCF="
  test("addChild".only):
    forAll: (root: NewRoot, oTree: Option[NewTree]) =>
      oTree.isDefined ==> {
        val tree    = oTree.get.take(1).clearVariations.pp
        val oldRoot = root.toRoot.addChild(tree.toBranch)
        assertEquals(oldRoot.toNewRoot.pp, root.addChild(tree).pp)
      }
