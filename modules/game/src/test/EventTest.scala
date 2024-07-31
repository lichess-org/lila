package lila.game

import cats.syntax.all.*
import chess.*
import chess.CoreArbitraries.given
import chess.bitboard.Bitboard
import org.scalacheck.Prop.{ forAll, propBoolean }
import play.api.libs.json.*

class EventTest extends munit.ScalaCheckSuite:

  test("PossibleMoves anti regression"):
    val moves = Map(Square.E2 -> Bitboard(Square.E4, Square.D4, Square.E3))
    val str   = stringFromPossibleMoves(moves)
    // If We somehow change the order of destinations, We may need to update this test
    assertEquals(str, "e2e3d4e4")

  test("PossibleMoves with empty map"):
    assertEquals(Event.PossibleMoves.json(Map.empty), JsNull)

  test("PossibleMoves writes every square"):
    forAll: (m: Map[Square, Bitboard]) =>
      m.nonEmpty ==> {
        val str          = stringFromPossibleMoves(m)
        val totalSquares = m.values.foldLeft(0)(_ + _.count) + m.size
        // str length = total squares * 2 + spaces in between
        assertEquals(str.length, totalSquares * 2 + m.size - 1)
      }

  private def stringFromPossibleMoves(moves: Map[Square, Bitboard]): String =
    Event.PossibleMoves.json(moves) match
      case JsString(str) => str
      case _             => failSuite("Expected JsString")
