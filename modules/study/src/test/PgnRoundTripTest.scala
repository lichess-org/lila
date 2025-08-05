package lila.study
import chess.format.pgn.PgnStr

import scala.language.implicitConversions

import lila.core.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.tree.{ NewRoot, Root }

import BSONHandlers.given
import Helpers.*

class PgnRoundTripTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, None, false)

  test("roundtrip"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImport.result(pgn, List(user)).toOption.get
        val dumped = rootToPgn(imported.root)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  test("NewTree roundtrip".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImportNew(pgn, List(user)).toOption.get
        val dumped = rootToPgn(imported.root)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]
  val w = new Writer

  test("roundtrip with BSONHandlers"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImport.result(pgn, List(user)).toOption.get
        val afterBson = treeBson.reads(treeBson.writes(w, imported.root))
        val dumped = rootToPgn(afterBson)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  test("NewTree roundtrip with BSONHandlers".ignore):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImportNew(pgn, List(user)).toOption.get
        val afterBson = newTreeBson.reads(newTreeBson.writes(w, imported.root))
        val dumped = rootToPgn(afterBson)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  extension (pgn: String)
    def cleanTags: String =
      pgn.split("\n").map(_.trim).filterNot(x => x.startsWith("[") || x.isBlank).mkString("\n")
