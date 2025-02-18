package lila.game

import chess.*
import chess.bitboard.Bitboard
import chess.variant.Crazyhouse
import org.scalacheck.Prop.{ forAll, propBoolean }
import play.api.libs.json.*
import chess.CoreArbitraries.given
import JsonView.{ *, given }
import lila.common.Json.given

class EventTest extends munit.ScalaCheckSuite:

  import Arbitraries.given

  test("Move anti regression"):
    forAll: (move: Event.Move) =>
      val data = move.data
      assertEquals(data.str("uci"), s"${move.orig.key}${move.dest.key}".some)
      assertEquals(data.str("san"), move.san.value.some)
      assertEquals(data.str("fen"), move.fen.value.some)
      assertEquals(data.int("ply"), move.state.turns.value.some)
      val destsJson = Event.PossibleMoves.oldJson(move.possibleMoves)
      assert(destsJson == JsNull || move.data.obj("dests") == destsJson.some)
      assertFalseOrSome(data)("check", move.check.value)
      assertFalseOrSome(data)("threefold", move.threefold)
      assertFalseOrSome(data)("wDraw", move.state.whiteOffersDraw)
      assertFalseOrSome(data)("bDraw", move.state.blackOffersDraw)
      assertEquals(move.data.obj("clock"), move.clock.map(_.data))
      assertEquals(move.data.obj("status"), move.state.status.map(summon[OWrites[Status]].writes))
      assertEquals(move.data.str("winner"), move.state.winner.map(_.name))
      assertEquals(move.data.obj("promotion"), move.promotion.map(_.data))
      assertEquals(move.data.obj("enpassant"), move.enpassant.map(_.data))
      assertEquals(move.data.obj("castle"), move.castle.map(_.data))
      assertEquals(move.data.obj("crazyhouse"), move.crazyData.map(summon[OWrites[Crazyhouse.Data]].writes))
      assertEquals(move.data.str("drops"), move.possibleDrops.map(_.map(_.key).mkString))

  private def assertFalseOrSome(obj: JsValue)(field: String, value: Boolean)(implicit loc: munit.Location) =
    assert(!value || obj.boolean(field) == true.some)

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
    forAll: (event: Event.Enpassant) =>
      assertEquals(event.data.str("key"), event.pos.key.some)
      assertEquals(event.data.str("color"), event.color.name.some)

  test("Castling anti regression"):
    forAll: (event: Event.Castling) =>
      assertEquals(event.typ, "castling")
      assertEquals(event.data.str("color"), event.color.name.some)
      assertEquals(event.data.arr("king"), Json.arr(event.castle.king.key, event.castle.kingTo.key).some)
      assertEquals(event.data.arr("rook"), Json.arr(event.castle.rook.key, event.castle.rookTo.key).some)

  test("RedirectOwner anti regression"):
    forAll: (event: Event.RedirectOwner) =>
      assertEquals(event.data.str("id"), event.id.value.some)
      assertEquals(event.data.str("url"), s"/${event.id}".some)
      assertEquals(event.data.obj("cookie"), event.cookie)
