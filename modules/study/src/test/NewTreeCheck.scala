package lila.study

import chess.CoreArbitraries.given
import chess.format.UciPath
import chess.format.pgn.{ Glyph, PgnStr }
import org.scalacheck.Prop.{ forAll, propBoolean }

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.study.BSONHandlers.given
import lila.tree.Node.Shapes
import lila.tree.{ Branch, NewRoot, NewTree, Node, Root, Clock }

import StudyArbitraries.{ *, given }

@munit.IgnoreSuite
class NewTreeCheck extends munit.ScalaCheckSuite:

  import Helpers.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  given Conversion[Bdoc, Reader]   = Reader(_)

  //
  // BSONHandlers
  //

  val treeBson    = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]
  val w           = new Writer

  test("Tree.writes.Tree.reads == identity"):
    forAll: (x: NewRoot) =>
      val root = x.toRoot
      val bdoc = treeBson.writes(w, root)
      val y    = treeBson.reads(bdoc)
      assertEquals(y, root)

  test("NewTree.writes.Tree.reads == identity"):
    forAll: (x: NewRoot) =>
      val bdoc = newTreeBson.writes(w, x)
      val y    = treeBson.reads(bdoc).toNewRoot
      assertEquals(y, x)

  test("Tree.writes.NewTree.reads == identity"):
    forAll: (x: NewRoot) =>
      val bdoc = treeBson.writes(w, x.toRoot)
      val y    = newTreeBson.reads(bdoc)
      assertEquals(y, x)

  test("NewTree.writes.NewTree.reads == identity"):
    forAll: (x: NewRoot) =>
      val bdoc = newTreeBson.writes(w, x)
      val y    = newTreeBson.reads(bdoc)
      assertEquals(y, x)

  test("Tree.writes.Tree.reads == identity"):
    forAll: (x: NewRoot) =>
      val root = x.toRoot
      val bdoc = treeBson.writes(w, root)
      val y    = treeBson.reads(bdoc)
      assertEquals(y, root)
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
    forAll: (rp: RootWithPath, clock: Option[Clock]) =>
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
    forAll: (root: NewRoot, c: Option[Clock]) =>
      val oldRoot = root.toRoot
      oldRoot.updateMainlineLast(_.copy(clock = c)).toNewRoot == root.updateMainlineLast(_.copy(clock = c))

  // test("takeMainlineWhile"):
  //   forAll: (root: NewRoot, f: Option[Clock] => Boolean) =>
  //     val c = root
  //     val x = c.toRoot.takeMainlineWhile(b => f(b.clock)).toNewRoot
  //     val y = c.takeMainlineWhile(b => f(b.clock))
  //     // The current tree always take the first child of the root despite the predicate
  //     // so, We have to ignore the case where the first child doesn't satisfy the predicate
  //     c.tree.exists(b => f(b.value.clock)) ==> (x == y)

  test("current tree's bug with takeMainlineWhile".ignore):
    val pgn     = "1. d4 d5 2. e4 e5"
    val newRoot = StudyPgnImportNew(pgn, Nil).toOption.get.root
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

  //
  // JSON
  //

  test("defaultJsonString"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      val x       = Node.defaultNodeJsonWriter.writes(oldRoot)
      val y       = NewRoot.defaultNodeJsonWriter.writes(root)
      assertEquals(x, y)

  test("minimalNodeJsonWriter"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      val x       = Node.minimalNodeJsonWriter.writes(oldRoot)
      val y       = NewRoot.minimalNodeJsonWriter.writes(root)
      assertEquals(x, y)

  test("partitionTreeJsonWriter"):
    forAll: (root: NewRoot) =>
      val oldRoot = root.toRoot
      val x       = Node.partitionTreeJsonWriter.writes(oldRoot)
      val y       = NewRoot.partitionTreeJsonWriter.writes(root)
      assertEquals(x, y)
