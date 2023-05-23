package lila.study

import cats.syntax.all.*
import chess.{ Centis, ErrorStr, Node as PgnNode }
import chess.format.pgn.{ PgnStr, Tags }

import lila.common.LightUser
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root }

import lila.db.BSON
import BSONHandlers.given
import lila.db.BSON.Writer
import lila.db.BSON.Reader
import lila.db.dsl.Bdoc

class PgnRoundTripTest extends lila.common.LilaTest:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, false)

  def rootToPgn(root: Root) = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.WithFlags(true, true, true, true, false))
    .render

  import PgnImport.*

  test("roundtrip"):
    PgnFixtures.roundTrip
      .map(cleanTags)
      .foreach: pgn =>
        val imported = PgnImport(pgn, List(user)).toOption.get
        val dumped   = rootToPgn(imported.root)
        assertEquals(dumped.value, pgn.trim)

  given Conversion[Bdoc, Reader] = Reader(_)
  val treeBson                   = summon[BSON[Root]]
  val w = new Writer

  test("roundtrip with BSONHandlers"):
    PgnFixtures.roundTrip
      .map(cleanTags)
      .foreach: pgn =>
        val imported = PgnImport(pgn, List(user)).toOption.get
        val y        = treeBson.reads(treeBson.writes(w, imported.root))
        val dumped   = rootToPgn(y)
        assertEquals(dumped.value, pgn.trim)

  def cleanTags(pgn: String): String =
    pgn.split("\n").filter(!_.startsWith("[")).mkString("\n")
