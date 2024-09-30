package lila.game

import cats.syntax.all.*
import chess.*
import chess.CoreArbitraries.given
import chess.bitboard.Bitboard
import lila.core
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
    var event = Event.Enpassant(pos = chess.Square.A1, color = chess.Color.White)
    assertEquals(event.typ, "enpassant")
    assertEquals(
      event.data,
      Json.obj(
        "key"   -> "a1",
        "color" -> "white"
      )
    )
    event = Event.Enpassant(pos = chess.Square.C3, color = chess.Color.Black)
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
      castle = chess.Move.Castle(
        king = chess.Square.E1,
        kingTo = chess.Square.C1,
        rook = chess.Square.A1,
        rookTo = chess.Square.D1
      ),
      color = chess.Color.White
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
      castle = chess.Move.Castle(
        king = chess.Square.E8,
        kingTo = chess.Square.G8,
        rook = chess.Square.H8,
        rookTo = chess.Square.F8
      ),
      color = chess.Color.Black
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
      color = chess.Color.White,
      id = lila.core.id.GameFullId(
        gameId = lila.core.id.GameId.take("abcdefgh"),
        playerId = lila.core.id.GamePlayerId("1234")
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
      color = chess.Color.Black,
      id = lila.core.id.GameFullId(
        gameId = lila.core.id.GameId.take("12345678"),
        playerId = lila.core.id.GamePlayerId("abcd")
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
