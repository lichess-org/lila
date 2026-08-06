package lila.study

import chess.format.pgn.{ Glyph as BaseGlyph, Glyphs as BaseGlyphs, PgnStr }

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.*
import lila.study.BSONHandlers.given
import lila.tree.Node.{ Comment, Glyphs as NodeGlyphs }
import lila.tree.{ Branch, Root, evals }

// in lila.study to have access to PgnImport
class BsonHandlersTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  given Conversion[Bdoc, Reader] = Reader(_)

  import Helpers.*

  val treeBson = summon[BSON[Root]]
  val w = new Writer

  test("Tree writes.reads == identity"):
    List(PgnFixtures.pgn8).foreach: pgn =>
      val x = StudyPgnImport.result(pgn, Nil).toOption.get.root
      val y = treeBson.reads(treeBson.writes(w, x))
      assertEquals(y, x.withoutClockTrust)

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

  test("node comp hints survive round trip"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val withComp = root.updateChildrenAt(path, _.setComp).get
    val roundTripped = treeBson.reads(treeBson.writes(w, withComp))
    assert(roundTripped.nodeAt(path).exists(_.comp))

  test("annotation comp hints survive round trip"):
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

  test("eval provenance survives round trip"):
    val root = StudyPgnImport.result(PgnFixtures.pgn3, Nil).toOption.get.root
    val path = root.mainlinePath
    val static = root.updateChildrenAt(path, _.copy(eval = evals.initial.copy(static = true).some)).get
    val legacy = root.updateChildrenAt(path, _.copy(eval = evals.initial.some)).get
    assert(treeBson.reads(treeBson.writes(w, static)).nodeAt(path).flatMap(_.eval).exists(_.static))
    assert(!treeBson.reads(treeBson.writes(w, legacy)).nodeAt(path).flatMap(_.eval).exists(_.static))

    val staticNode = static.nodeAt(path).collect { case branch: Branch => branch }.get
    val legacyNode = legacy.nodeAt(path).collect { case branch: Branch => branch }.get
    val staticBson = BSONHandlers.writeBranch(staticNode)
    assertEquals(
      staticBson.getAsOpt[Bdoc]("e").flatMap(_.getAsOpt[chess.eval.Score]("st")),
      staticNode.eval.flatMap(_.score)
    )
    assertEquals(staticBson.getAsOpt[Boolean]("st"), None)
    assertEquals(
      BSONHandlers.writeBranch(legacyNode).getAsOpt[chess.eval.Score]("e"),
      legacyNode.eval.flatMap(_.score)
    )

  test("engine continuations stay first after their shared initial branch"):
    val userRoot = StudyPgnImport.result("1. e4 e5 2. Nf3 Nc6", Nil).toOption.get.root
    val engineLine = StudyPgnImport
      .result("1. e4 c5 2. Nf3 d6", Nil)
      .toOption
      .get
      .root
      .children
      .first
      .map: line =>
        line.copy(children = line.children.updateAllWith(_.setComp)).setComp
      .get
    val merged = ServerEval.mergeAnalysis(userRoot, chess.format.UciPath.root, engineLine)
    val sharedInitial = merged.children.first.get
    assert(!sharedInitial.comp)
    assertEquals(sharedInitial.children.first.map(_.move.san.value), Some("c5"))
    assert(sharedInitial.children.first.exists(_.comp))
