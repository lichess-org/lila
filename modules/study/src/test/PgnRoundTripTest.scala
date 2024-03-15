package lila.study
import chess.format.pgn.{ PgnStr, Tags }

import scala.language.implicitConversions

import lila.common.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.tree.Root

import BSONHandlers.given

class PgnRoundTripTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, None, false)

  def rootToPgn(root: Root) = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.WithFlags(true, true, true, true, false))
    .render

  import PgnImport.*

  test("roundtrip"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = PgnImport(pgn, List(user)).toOption.get
        val dumped   = rootToPgn(imported.root)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson                   = summon[BSON[Root]]
  val w                          = new Writer

  test("roundtrip with BSONHandlers"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported  = PgnImport(pgn, List(user)).toOption.get
        val afterBson = treeBson.reads(treeBson.writes(w, imported.root))
        val dumped    = rootToPgn(afterBson)
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  extension (pgn: String)
    def cleanTags: String =
      pgn.split("\n").map(_.trim).filterNot(x => x.startsWith("[") || x.isBlank).mkString("\n")
