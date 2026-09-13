package lila.study
import chess.format.pgn.{ PgnStr, Tag, Tags }

import scala.language.implicitConversions

import lila.core.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.tree.Root

import BSONHandlers.given
import Helpers.*

class PgnRoundTripTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val user = LightUser.fallback(UserName("Annotator"))

  // the dump only reads the Annotator tag, and the rest would render a result terminator
  def annotatorOf(tags: Tags) = Tags(tags.value.filter(_.name == Tag.Annotator))

  test("roundtrip"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImport.result(pgn, List(user)).toOption.get
        val dumped = rootToPgn(imported.root, annotatorOf(imported.tags))
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson = summon[BSON[Root]]
  val w = new Writer

  test("roundtrip with BSONHandlers"):
    PgnFixtures.roundTrip
      .foreach: pgn =>
        val imported = StudyPgnImport.result(pgn, List(user)).toOption.get
        val afterBson = treeBson.reads(treeBson.writes(w, imported.root))
        val dumped = rootToPgn(afterBson, annotatorOf(imported.tags))
        assertEquals(dumped.value.cleanTags, pgn.cleanTags)

  extension (pgn: String)
    def cleanTags: String =
      pgn.split("\n").map(_.trim).filterNot(x => x.startsWith("[") || x.isBlank).mkString("\n")
