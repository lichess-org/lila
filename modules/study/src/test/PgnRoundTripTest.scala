package lila.study

import chess.{ Centis, ErrorStr, Node as PgnNode }
import chess.format.pgn.{ PgnStr, Tags }

import lila.core.LightUser
import lila.core.game.{ ImportData, ImportReady }
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root }

import lila.db.BSON
import BSONHandlers.given
import lila.db.BSON.Writer
import lila.db.BSON.Reader
import lila.db.dsl.Bdoc
import Helpers.*
import lila.tree.NewRoot

class PgnRoundTripTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, None, false)

  import Helpers.{ importerStub, newImporterStub }

  test("roundtrip".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = importerStub(pgn, List(user)).toOption.get
        val dumped   = rootToPgn(imported.root)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  test("NewTree roundtrip".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = newImporterStub(pgn, List(user)).toOption.get
        val dumped   = rootToPgn(imported.root)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson                   = summon[BSON[Root]]
  val newTreeBson                = summon[BSON[NewRoot]]
  val w                          = new Writer

  test("roundtrip with BSONHandlers".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported  = importerStub(pgn, List(user)).toOption.get
        val afterBson = treeBson.reads(treeBson.writes(w, imported.root))
        val dumped    = rootToPgn(afterBson)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  test("NewTree roundtrip with BSONHandlers".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported  = newImporterStub(pgn, List(user)).toOption.get
        val afterBson = newTreeBson.reads(newTreeBson.writes(w, imported.root))
        val dumped    = rootToPgn(afterBson)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  extension (pgn: String)
    def cleanTags: String =
      pgn.split("\n").map(_.trim).filterNot(x => x.startsWith("[") || x.isBlank).mkString("\n")
