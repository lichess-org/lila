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

  test("Root conversion check"):
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

  test("forceVariationAt"):
    forAll: (rp: RootWithPath, force: Boolean) =>
      val (root, path) = rp
      !path.isEmpty ==> {
        val oldRoot = root.toRoot
        oldRoot.forceVariationAt(force, path).map(_.toNewRoot) == root.modifyBranchAt(
          path,
          _.copy(forceVariation = force)
        )
      }

  test("updateMainlineLast"):
    forAll: (root: NewRoot, c: Option[Centis]) =>
      val oldRoot = root.toRoot
      oldRoot.updateMainlineLast(_.copy(clock = c)).toNewRoot == root.updateMainlineLast(_.copy(clock = c))

  test("takeMainlineWhile".ignore):
    forAll: (root: NewRoot, f: Option[Centis] => Boolean) =>
      val c = root
      val x = c.toRoot.takeMainlineWhile(b => f(b.clock)).toNewRoot
      val y = c.takeMainlineWhile(b => f(b.clock))
      // The current tree always take the first child of the root despite the predicate
      // so, We have to ignore the case where the first child doesn't satisfy the predicate
      c.tree.exists(b => f(b.value.clock)) ==> (x == y)

  test("current tree's bug with takeMainlineWhile".ignore):
    val pgn     = "1. d4 d5 2. e4 e5"
    val newRoot = NewPgnImport(pgn, Nil).toOption.get.root
    val oldRoot = newRoot.toRoot
    assert(oldRoot.takeMainlineWhile(_.clock.isDefined).children.isEmpty)

  test("clearVariations"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.clearVariations.toNewRoot == root.clearVariations

  test("mainline"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.mainline.map(NewTree.fromBranch) == root.mainlineValues

  test("lastMainlinePly"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.lastMainlinePly == root.lastMainlinePly

  test("lastMainlinePlyOf"):
    forAll: (rp: RootWithPath) =>
      val (root, path) = rp
      val oldRoot      = root.toRoot
      oldRoot.lastMainlinePlyOf(path) == root.lastMainlinePlyOf(path)

  test("mainlinePath"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      oldRoot.mainlinePath == root.mainlinePath

  test("lastMainlineNode"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      root.lastMainlineNode.isEmpty ||
      NewTree
        .fromBranch(oldRoot.lastMainlineNode.asInstanceOf[Branch]) == root.lastMainlineNode.map(_.value).get

  test("nodeAt"):
    forAll: (rp: RootWithPath) =>
      val (root, path) = rp
      path.nonEmpty ==> {
        val oldRoot = root.toRoot
        oldRoot.nodeAt(path).isEmpty == root.nodeAt(path).isEmpty
      }

  test("addNodeAt"):
    forAll: (rp: RootWithPath, oTree: Option[NewTree]) =>
      val (root, path) = rp

      oTree.isDefined && path.nonEmpty ==> {
        val tree    = oTree.get.withoutVariations
        val oldRoot = root.toRoot.withChildren(_.addNodeAt(tree.toBranch, path))
        val x       = oldRoot.map(_.toNewRoot)
        val y       = root.addNodeAt(path, tree)
        // We compare only size because We have different merging strategies
        // In the current tree, We put the added node/ the merged node at the end of the children
        // Int the new tree, if the node already exists, We merge the node at the same position as the existing node
        // if the node's id is unique, We put the node at the end of the variations
        // I believe the new tree's strategy is more reasonable
        assertEquals(x.isDefined, y.isDefined)
        assertEquals(x.fold(0)(_.size), y.fold(0)(_.size))
      }

  // similar to addNodeAt, We only compare size
  test("addChild"):
    forAll: (root: NewRoot, oTree: Option[NewTree]) =>
      oTree.isDefined ==> {
        val tree = oTree.get.withoutVariations
        root.toRoot.addChild(tree.toBranch).toNewRoot.size == root.addChild(tree).size
      }
