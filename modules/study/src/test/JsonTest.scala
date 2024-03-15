package lila.study

import chess.variant.Standard

import lila.common.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.study.Helpers.*
import lila.tree.Node.partitionTreeJsonWriter
import lila.tree.Root

import PgnImport.*
import BSONHandlers.given

class JsonTest extends munit.FunSuite:

  val user = LightUser(UserId("nt9"), UserName("nt9"), None, None, false)

  test("Json writes"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val imported = PgnImport(pgn, List(user)).toOption.get.root.cleanCommentIds
        val json     = writeTree(imported)
        assertEquals(json, expected)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson                   = summon[BSON[Root]]
  val w                          = new Writer

  test("Json writes with BSONHandlers"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val imported  = PgnImport(pgn, List(user)).toOption.get.root.cleanCommentIds
        val afterBson = treeBson.reads(treeBson.writes(w, imported))
        val json      = writeTree(afterBson)
        assertEquals(json, expected)

  extension (root: Root)
    def cleanCommentIds: Root =
      NewRootC.fromRoot(root).cleanup.toRoot

  def writeTree(tree: Root) = partitionTreeJsonWriter
    .writes(lila.study.TreeBuilder(tree, Standard))
    .toString
