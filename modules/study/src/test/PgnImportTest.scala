package lila.study

import cats.syntax.all.*
import chess.{ ErrorStr, Ply }
import chess.format.pgn.{ Tags, PgnStr }

import lila.common.LightUser
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import scala.language.implicitConversions

class PgnImportTest extends lila.common.LilaTest:

  import PgnImport.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val pgn = """
  {This move:} 1.e4! {, was considered by R.J.Fischer as "best by test"}
    ( {This other move:} {looks pretty} 1.d4?! {not.} )
    ( ;Neither does :
      ;this or that
      {or whatever}
      1.b4?! {this one} ) 1... e5 2 c4
  """

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, false)

  test("import pgn"):
    val x: Validated[ErrorStr, Result] = PgnImport(pgn, List(user))
    assertMatch(x) { case Validated.Valid(parsed: Result) =>
      parsed.tags == Tags.empty &&
      parsed.root.children.nodes.size == 3 &&
      parsed.root.ply == Ply.initial
    }

  test("import a simple pgn"):
    val x: Validated[ErrorStr, Result] = PgnImport("1.d4 d5 2.e4 e5", List(user))
    assertMatch(x) { case Validated.Valid(parsed: Result) =>
      parsed.tags == Tags.empty &&
      parsed.root.children.nodes.size == 1 &&
      parsed.root.ply == Ply.initial
    }
