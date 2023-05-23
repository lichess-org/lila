package lila.study

import lila.tree.Node.partitionTreeJsonWriter
import lila.common.LightUser
import PgnImport.*
import lila.tree.Root
import chess.variant.Standard
import lila.tree.NewRoot

import cats.syntax.all.*
import monocle.syntax.all.*
import lila.study.Helpers.*

class JsonTest extends munit.FunSuite:

  val user = LightUser(UserId("nt9"), UserName("nt9"), None, false)

  test("Json writes"):
    PgnFixtures.roundTrip
      .zip(JsonFixtures.all)
      .foreach: (pgn, expected) =>
        val imported = PgnImport(pgn, List(user)).toOption.get.root.cleanCommentIds
        val json     = writeTree(imported)
        assertEquals(json, expected)

  extension (root: Root)
    def cleanCommentIds: Root =
      NewRootC.fromRoot(root).cleanup.toRoot

  def writeTree(tree: Root) = partitionTreeJsonWriter
    .writes:
      lila.study.TreeBuilder(tree, Standard)
    .toString
