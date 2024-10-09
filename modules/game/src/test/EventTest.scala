package lila.game

import cats.syntax.all.*
import chess.*
import chess.CoreArbitraries.given
import chess.bitboard.Bitboard
import chess.variant.Crazyhouse
import lila.core.id.*
import org.scalacheck.Prop.{ forAll, propBoolean }
import play.api.libs.json.*
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

class EventTest extends munit.ScalaCheckSuite:

  test("Move anti regression"):
    var event = Event.Move(
      orig = Square.A1,
      dest = Square.A2,
      san = format.pgn.SanStr("abcdef"),
      fen = format.BoardFen("ghijkl"),
      check = Check(false),
      threefold = false,
      promotion = None,
      enpassant = None,
      castle = None,
      state = Event.State(
        turns = Ply(1),
        status = None,
        winner = None,
        whiteOffersDraw = false,
        blackOffersDraw = false
      ),
      clock = None,
      possibleMoves = Map.empty,
      possibleDrops = None,
      crazyData = None
    )
    assertEquals(event.typ, "move")
    assertEquals(
      event.data,
      Json.obj(
        "uci"   -> "a1a2",
        "san"   -> "abcdef",
        "fen"   -> "ghijkl",
        "ply"   -> 1,
        "dests" -> None
      )
    )
    event = Event.Move(
      orig = Square.C7,
      dest = Square.C8,
      san = format.pgn.SanStr("123456"),
      fen = format.BoardFen("789012"),
      check = Check(true),
      threefold = true,
      promotion = Some(
        Event.Promotion(
          role = Rook,
          pos = Square.C8
        )
      ),
      enpassant = Some(
        Event.Enpassant(
          pos = Square.C8,
          color = Color.Black
        )
      ),
      castle = Some(
        Event.Castling(
          castle = Move.Castle(
            king = Square.E1,
            kingTo = Square.C1,
            rook = Square.A1,
            rookTo = Square.D1
          ),
          color = Color.Black
        )
      ),
      state = Event.State(
        turns = Ply(5),
        status = Some(Status.Mate),
        winner = Some(Color.Black),
        whiteOffersDraw = true,
        blackOffersDraw = true
      ),
      clock = Some(
        Event.Clock(
          white = Centis(25),
          black = Centis(35),
          nextLagComp = Some(Centis(10))
        )
      ),
      possibleMoves = Map(Square.E2 -> Bitboard(Square.E4, Square.D4, Square.E3)),
      possibleDrops = Some(List(Square.G1, Square.F2)),
      crazyData = Some(
        Crazyhouse.Data(
          pockets = ByColor(
            Crazyhouse.Pocket(1, 2, 3, 4, 5),
            Crazyhouse.Pocket(6, 7, 8, 9, 10)
          ),
          promoted = Bitboard(Square.B8, Square.A8)
        )
      )
    )
    assertEquals(event.typ, "move")
    assertEquals(
      event.data,
      Json.obj(
        "uci" -> "c7c8",
        "san" -> "123456",
        "fen" -> "789012",
        "ply" -> 5,
        "dests" -> Json.obj(
          "e2" -> "e3d4e4"
        ),
        "clock" -> Json.obj(
          "white" -> 0.25,
          "black" -> 0.35,
          "lag"   -> 10
        ),
        "status" -> Json.obj(
          "id"   -> 30,
          "name" -> "mate"
        ),
        "winner"    -> "black",
        "check"     -> true,
        "threefold" -> true,
        "wDraw"     -> true,
        "bDraw"     -> true,
        "crazyhouse" -> Json.obj(
          "pockets" -> Json.arr(
            Json.obj(
              "pawn"   -> 1,
              "knight" -> 2,
              "bishop" -> 3,
              "rook"   -> 4,
              "queen"  -> 5
            ),
            Json.obj(
              "pawn"   -> 6,
              "knight" -> 7,
              "bishop" -> 8,
              "rook"   -> 9,
              "queen"  -> 10
            )
          )
        ),
        "drops" -> "g1f2",
        "promotion" -> Json.obj(
          "key"        -> "c8",
          "pieceClass" -> "rook"
        ),
        "enpassant" -> Json.obj(
          "key"   -> "c8",
          "color" -> "black"
        ),
        "castle" -> Json.obj(
          "king"  -> Json.arr("e1", "c1"),
          "rook"  -> Json.arr("a1", "d1"),
          "color" -> "black"
        )
      )
    )

  test("Move writes every square"):
    forAll: (orig: Square, dest: Square, turns: Int, possibleMoves: Map[Square, Bitboard]) =>
      var event = Event.Move(
        orig = orig,
        dest = dest,
        san = format.pgn.SanStr("abcdef"),
        fen = format.BoardFen("ghijkl"),
        check = Check(false),
        threefold = false,
        promotion = None,
        enpassant = None,
        castle = None,
        state = Event.State(
          turns = Ply(turns),
          status = None,
          winner = None,
          whiteOffersDraw = false,
          blackOffersDraw = false
        ),
        clock = None,
        possibleMoves = possibleMoves,
        possibleDrops = None,
        crazyData = None
      )
      assertEquals(event.typ, "move")
      assertEquals(
        event.data,
        Json.obj(
          "uci"   -> s"${orig.key}${dest.key}",
          "san"   -> "abcdef",
          "fen"   -> "ghijkl",
          "ply"   -> turns,
          "dests" -> Event.PossibleMoves.oldJson(possibleMoves)
        )
      )

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
    forAll: (square: Square, color: Color) =>
      val event = Event.Enpassant(square, color)
      assertEquals(event.data.str("key"), square.key.some)
      assertEquals(event.data.str("color"), color.name.some)

  // TODO move to scalachess
  extension (rank: Rank) def squares: List[Square] = Square.all.filter(_.rank == rank)
  given Arbitrary[Event.Castling] = Arbitrary {
    for
      color  <- Arbitrary.arbitrary[Color]
      king   <- Gen.oneOf(color.backRank.squares)
      kingTo <- Gen.oneOf(color.fold(List(Square.G1, Square.C1), List(Square.G8, Square.C8)))
      rookTo <- Gen.oneOf(color.fold(List(Square.F1, Square.D1), List(Square.F8, Square.D8)))
      rook   <- Gen.oneOf(color.backRank.squares.filter(_ != king))
      castle = Move.Castle(king, kingTo, rook, rookTo)
    yield Event.Castling(castle, color)
  }

  test("Castling anti regression"):
    forAll: (event: Event.Castling) =>
      assertEquals(event.data.str("color"), event.color.name.some)
      assertEquals(event.data.arr("king"), Json.arr(event.castle.king.key, event.castle.kingTo.key).some)
      assertEquals(event.data.arr("rook"), Json.arr(event.castle.rook.key, event.castle.rookTo.key).some)

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

  test("RedirectOwner writes every color"):
    var id = GameFullId(
      gameId = GameId.take("12345678"),
      playerId = GamePlayerId("abcd")
    )
    forAll: (color: Color) =>
      var event = Event.RedirectOwner(
        color = color,
        id = id,
        cookie = None
      )
      assertEquals(event.typ, "redirect")
      assertEquals(
        event.data,
        Json.obj(
          "id"  -> s"$id",
          "url" -> s"/$id"
        )
      )
      assertEquals(event.only, Some(color))
