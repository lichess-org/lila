package lila.game

import cats.syntax.all.*
import chess.*
import chess.CoreArbitraries.given
import chess.bitboard.Bitboard
import lila.core.id.*
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

  test("Enpassant anti regression"):
    var event = Event.Enpassant(pos = Square.A1, color = Color.White)
    assertEquals(event.typ, "enpassant")
    assertEquals(
      event.data,
      Json.obj(
        "key"   -> "a1",
        "color" -> "white"
      )
    )
    event = Event.Enpassant(pos = Square.C3, color = Color.Black)
    assertEquals(event.typ, "enpassant")
    assertEquals(
      event.data,
      Json.obj(
        "key"   -> "c3",
        "color" -> "black"
      )
    )

  test("Castling anti regression"):
    var event = Event.Castling(
      castle = Move.Castle(
        king = Square.E1,
        kingTo = Square.C1,
        rook = Square.A1,
        rookTo = Square.D1
      ),
      color = Color.White
    )
    assertEquals(event.typ, "castling")
    assertEquals(
      event.data,
      Json.obj(
        "king"  -> Json.arr("e1", "c1"),
        "rook"  -> Json.arr("a1", "d1"),
        "color" -> "white"
      )
    )
    event = Event.Castling(
      castle = Move.Castle(
        king = Square.E8,
        kingTo = Square.G8,
        rook = Square.H8,
        rookTo = Square.F8
      ),
      color = Color.Black
    )
    assertEquals(event.typ, "castling")
    assertEquals(
      event.data,
      Json.obj(
        "king"  -> Json.arr("e8", "g8"),
        "rook"  -> Json.arr("h8", "f8"),
        "color" -> "black"
      )
    )

  test("RedirectOwner anti regression"):
    var event = Event.RedirectOwner(
      color = Color.White,
      id = GameFullId(
        gameId = GameId.take("abcdefgh"),
        playerId = GamePlayerId("1234")
      ),
      cookie = None
    )
    assertEquals(event.typ, "redirect")
    assertEquals(
      event.data,
      Json.obj(
        "id"  -> "abcdefgh1234",
        "url" -> "/abcdefgh1234"
      )
    )
    assertEquals(event.only, Some(chess.Color.White))
    event = Event.RedirectOwner(
      color = Color.Black,
      id = GameFullId(
        gameId = GameId.take("12345678"),
        playerId = GamePlayerId("abcd")
      ),
      cookie = Some(Json.obj("cookie" -> "something"))
    )
    assertEquals(event.typ, "redirect")
    assertEquals(
      event.data,
      Json.obj(
        "id"     -> "12345678abcd",
        "url"    -> "/12345678abcd",
        "cookie" -> Json.obj("cookie" -> "something")
      )
    )
    assertEquals(event.only, Some(chess.Color.Black))
