package lila.study

import cats.syntax.all.*
import chess.{ Centis, ErrorStr }
import chess.format.pgn.{
  Dumper,
  Glyphs,
  ParsedPgn,
  San,
  Tags,
  PgnStr,
  PgnNodeData,
  Comment as ChessComment,
  Node as PgnNode
}
import chess.format.{ Fen, Uci, UciCharPair }
import chess.MoveOrDrop.*

import lila.common.LightUser
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import org.specs2.matcher.Matcher
import org.specs2.matcher.ValidatedMatchers
import org.specs2.mutable.Specification
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root }

class PgnImportTest extends Specification with ValidatedMatchers:

  import PgnImport.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  val pgn                          = """
  {This move:} 1.e4! {, was considered by R.J.Fischer as "best by test"}
    ( {This other move:} {looks pretty} 1.d4?! {not.} )
    ( ;Neither does :
      ;this or that
      {or whatever}
      1.b4?! {this one} ) 1... e5 2 c4
  """

  val user = LightUser(UserId("aaaa"), UserName("Annotator"), None, false)

  "apply" should {
    "valid pgn" in {

      val x: Validated[ErrorStr, Result] = PgnImport(pgn, List(user))
      x must beValid.like { (parsed: Result) =>
        parsed.tags must beEqualTo(Tags.empty)
      }
    }
  }
