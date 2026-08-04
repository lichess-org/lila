package lila.study

import lila.core.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.*
import lila.study.Helpers.*
import lila.tree.{ NewRoot, Node, Root }

import BSONHandlers.given
import play.api.libs.json.Json

class JsonTest extends munit.FunSuite:

  val user = LightUser.fallback(UserName("nt9"))

  test("Json writes"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result = StudyPgnImport.result(pgn, List(user)).toOption.get
        val imported = result.root.cleanCommentIds
        val json = writeTree(imported)
        assertEquals(json, expected)

  test("NewTree Json writes".ignore):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result = StudyPgnImportNew(pgn, List(user)).toOption.get
        val imported = result.root.cleanup
        val json = writeTree(imported)
        assertEquals(Json.parse(json), Json.parse(expected))

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]
  val w = new Writer

  test("Json writes with BSONHandlers"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result = StudyPgnImport.result(pgn, List(user)).toOption.get
        val imported = result.root.cleanCommentIds
        val afterBson = treeBson.reads(treeBson.writes(w, imported))
        val json = writeTree(afterBson)
        assertEquals(json, expected)

  test("NewTree Json writes with BSONHandlers".ignore):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result = StudyPgnImportNew(pgn, List(user)).toOption.get
        val imported = result.root
        val afterBson = newTreeBson.reads(newTreeBson.writes(w, imported))
        val json = writeTree(afterBson.cleanup)
        assertEquals(Json.parse(json), Json.parse(expected))

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
    val json = Node.partitionTreeWriter(tree, false)
    val expectedJson = Json.arr(
      Json.obj(
        "ply" -> 0,
        "fen" -> "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
      ),
      Json.obj(
        "ply" -> 1,
        "fen" -> "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        "uci" -> "e2e4",
        "san" -> "e4",
        "children" -> Json.arr(
          Json.obj(
            "ply" -> 2,
            "fen" -> "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "uci" -> "e7e5",
            "san" -> "e5",
            "forceVariation" -> true
          ),
          Json.obj(
            "ply" -> 2,
            "fen" -> "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "uci" -> "d7d5",
            "san" -> "d5"
          )
        )
      )
    )
    assertEquals(json, expectedJson)

  extension (root: Root)
    def cleanCommentIds: Root =
      root.toNewRoot.cleanup.toRoot

  def writeTree(tree: Root): String =
    Node.partitionTreeWriter(tree, false).toString

  def writeTree(tree: NewRoot): String =
    NewRoot.partitionTreeJsonWriter.writes(tree).toString
