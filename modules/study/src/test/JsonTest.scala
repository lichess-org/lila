package lila.study

import chess.variant.Variant
import play.api.libs.json.Json

import lila.core.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.study.Helpers.*
import lila.tree.{ NewRoot, Node, Root }

import BSONHandlers.given

class JsonTest extends munit.FunSuite:

  val user = LightUser(UserId("nt9"), UserName("nt9"), None, None, false)

  test("Json writes"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result   = StudyPgnImport(pgn, List(user)).toOption.get
        val imported = result.root.cleanCommentIds
        val json     = writeTree(imported, result.variant)
        assertEquals(json, expected)

  test("NewTree Json writes"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result   = StudyPgnImportNew(pgn, List(user)).toOption.get
        val imported = result.root.cleanup
        val json     = writeTree(imported, result.variant)
        assertEquals(Json.parse(json), Json.parse(expected))

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson                   = summon[BSON[Root]]
  val newTreeBson                = summon[BSON[NewRoot]]
  val w                          = new Writer

  test("Json writes with BSONHandlers"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result    = StudyPgnImport(pgn, List(user)).toOption.get
        val imported  = result.root.cleanCommentIds
        val afterBson = treeBson.reads(treeBson.writes(w, imported))
        val json      = writeTree(afterBson, result.variant)
        assertEquals(json, expected)

  test("NewTree Json writes with BSONHandlers"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val result    = StudyPgnImportNew(pgn, List(user)).toOption.get
        val imported  = result.root
        val afterBson = newTreeBson.reads(newTreeBson.writes(w, imported))
        val json      = writeTree(afterBson.cleanup, result.variant)
        assertEquals(Json.parse(json), Json.parse(expected))

  extension (root: Root)
    def cleanCommentIds: Root =
      root.toNewRoot.cleanup.toRoot

  def writeTree(tree: Root, variant: Variant) = Node.partitionTreeJsonWriter
    .writes(lila.study.TreeBuilder(tree, variant))
    .toString

  def writeTree(tree: NewRoot, variant: Variant) = NewRoot.partitionTreeJsonWriter
    .writes(lila.study.TreeBuilder(tree, variant))
    .toString
