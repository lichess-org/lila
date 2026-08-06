package lila.study

import chess.format.pgn.{ Glyph as BaseGlyph, Glyphs as BaseGlyphs, PgnStr }

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.*
import lila.study.BSONHandlers.given
import lila.tree.Node.{ Comment, Glyphs as NodeGlyphs }
import lila.tree.{ NewRoot, Root }

// in lila.study to have access to PgnImport
class BsonHandlersTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  given Conversion[Bdoc, Reader] = Reader(_)

  import Helpers.*

  val treeBson = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]
  val w = new Writer

  test("Tree writes.reads == identity"):
    List(PgnFixtures.pgn8).foreach: pgn =>
      val x = StudyPgnImport.result(pgn, Nil).toOption.get.root
      val y = treeBson.reads(treeBson.writes(w, x))
      assertEquals(y, x.withoutClockTrust)

  test("comp hints survive tree BSON round trip"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val withComp = root.updateChildrenAt(path, _.setComp).get
    val roundTripped = treeBson.reads(treeBson.writes(w, withComp))
    assert(roundTripped.nodeAt(path).exists(_.comp))

  test("annotation provenance survives tree BSON round trip"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val withServerAnnotations = root
      .updateChildrenAt(
        path,
        _.copy(
          glyphs = NodeGlyphs.fromBase(BaseGlyphs.fromList(BaseGlyph.find(4).toList), comp = true),
          comments = lila.tree.Node.Comments.empty + Comment(
            Comment.Id.make,
            chess.format.pgn.Comment("Computer comment"),
            Comment.Author.Lichess,
            comp = true
          )
        )
      )
      .get
    val roundTripped = treeBson.reads(treeBson.writes(w, withServerAnnotations))
    val node = roundTripped.nodeAt(path).get
    assert(node.glyphs.value.toList.exists(_.comp))
    assert(node.comments.value.exists(_.comp))

  test("old annotations without provenance default to non-comp"):
    val old = Reader:
      $doc(
        "g" -> $arr(4),
        "c" -> $doc(
          "id" -> "abcd",
          "text" -> "Old comment",
          "by" -> "l"
        )
      )
    assert(old.get[NodeGlyphs]("g").value.toList.forall(!_.comp))
    assert(!old.get[Comment]("c").comp)

  test("baking server evaluation materializes nodes and glyphs"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val withServerEval = root.copy(
      children = root.children.updateAllWith: node =>
        node.copy(
          comp = true,
          glyphs = NodeGlyphs
            .fromBase(BaseGlyphs.fromList(BaseGlyph.find(1).toList))
            .merge(NodeGlyphs.fromBase(BaseGlyphs.fromList(BaseGlyph.find(4).toList), comp = true))
        )
    )
    val baked = ServerEval.bake(withServerEval)
    val node = baked.nodeAt(path).get
    assert(baked.children.hasNonComp)
    assert(!node.comp)
    assertEquals(node.glyphs.toBase, BaseGlyphs.fromList(BaseGlyph.find(4).toList))
    assert(node.glyphs.value.toList.forall(!_.comp))

  test("deleting a node prunes comp ancestors and their server eval continuation"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val alternative = StudyPgnImport.result("1. d4 d5 2. e4 e6", Nil).toOption.get.root
    val continuation = alternative.nodeAt(path.parent).flatMap(_.children.first).get.setComp
    val withCompAncestors = root.copy(children = root.children.updateAllWith(_.setComp))
    val withUserLeaf = withCompAncestors
      .updateChildrenAt(path.parent, _.addChild(continuation))
      .flatMap(_.updateChildrenAt(path, _.copy(comp = false)))
      .get
    val pruned = withUserLeaf.withChildren(_.deleteNodeAtAndPruneComp(path)).get
    assert(pruned.children.isEmpty)

  test("deleting a node preserves comp ancestors containing another user node"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val alternative = StudyPgnImport.result("1. d4 d5 2. e4 e6 3. Nc3", Nil).toOption.get.root
    val continuation = alternative.nodeAt(path.parent).flatMap(_.children.first).get
    val withCompAncestors = root.copy(children = root.children.updateAllWith(_.setComp))
    val withUserNodes = withCompAncestors
      .updateChildrenAt(
        path.parent,
        _.addChild(
          continuation.copy(
            comp = true,
            children = continuation.children.updateAllWith(_.copy(comp = false))
          )
        )
      )
      .flatMap(_.updateChildrenAt(path, _.copy(comp = false)))
      .get
    val pruned = withUserNodes.withChildren(_.deleteNodeAtAndPruneComp(path)).get
    assert(pruned.pathExists(alternative.mainlinePath))

  test("NewTree writes.reads == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImportNew(pgn, Nil).toOption.get.root
      val y = newTreeBson.reads(newTreeBson.writes(w, x))
      assertEquals(x.withoutClockTrust, y)

  test("NewTree.reads.Tree.writes == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImport.result(pgn, Nil).toOption.get.root
      val bdoc = treeBson.writes(w, x)
      val y = newTreeBson.reads(bdoc)
      assertEquals(x.toNewRoot.cleanup.withoutClockTrust, y.cleanup)

  test("Tree.reads.NewTree.writes == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImportNew(pgn, Nil).toOption.get.root
      val bdoc = newTreeBson.writes(w, x)
      val y = treeBson.reads(bdoc)
      assertEquals(y.toNewRoot.cleanup, x.cleanup.withoutClockTrust)

  test("forceVariation and node ordering"):
    // 1. e2e4 (e7e5 FV) d7d5
    val tree = treeBson.reads:
      $doc(
        "_" -> $doc("p" -> 0, "f" -> "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
        "/?" -> $doc(
          "p" -> 1,
          "u" -> "e2e4",
          "s" -> "e4",
          "f" -> "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        ),
        "/?WG" -> $doc(
          "p" -> 2,
          "u" -> "e7e5",
          "s" -> "e5",
          "f" -> "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
          "fv" -> true
        ),
        "/?VF" -> $doc(
          "p" -> 2,
          "u" -> "d7d5",
          "s" -> "d5",
          "f" -> "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
        )
      )
    val mainlineStr = tree.mainlineNodeList.toString
    val expectedMainlineStr =
      "List(0 List(), 1.e4 (Branches: List(2.e5 FV (Branches: List()), 2.d5 (Branches: List()))))"
    assertEquals(mainlineStr, expectedMainlineStr)

    val treeStr = tree.toString
    val expectedTreeStr =
      "0 List(1.e4 (Branches: List(2.e5 FV (Branches: List()), 2.d5 (Branches: List()))))"
    assertEquals(treeStr, expectedTreeStr)
