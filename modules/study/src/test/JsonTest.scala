package lila.study

import lila.tree.Node.partitionTreeJsonWriter
import lila.common.LightUser
import PgnImport.*
import lila.tree.Root
import chess.variant.Standard
import lila.tree.NewRoot

import monocle.syntax.all.*
import lila.study.Helpers.*

import lila.db.BSON
import BSONHandlers.given
import lila.db.BSON.Writer
import lila.db.BSON.Reader
import lila.db.dsl.Bdoc

class JsonTest extends munit.FunSuite:

  val user = LightUser(UserId("nt9"), UserName("nt9"), None, false)

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
