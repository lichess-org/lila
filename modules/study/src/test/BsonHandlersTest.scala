package lila.study

import chess.format.pgn.PgnStr

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.*
import lila.study.BSONHandlers.given
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
