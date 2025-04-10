package lila.relay

import chess.format.pgn.{ Parser, PgnStr, San, Std, Tags }
import RelayPush.*

class RelayPushTest extends munit.FunSuite:

  val validWrongMoves = List("e4", "e5", "d4", "d5").map("K" + _)

  test("fail before last move is fatal"):
    val pgns = validWrongMoves.map(m => PgnStr(s"1.e4 e5 2.e3 $m"))
    assert(pgns.forall(validate(_).isLeft))

  test("the last move with king in the center is not fatal"):
    val pgns = validWrongMoves.map(m => PgnStr(s"1.e4 $m"))
    assert(pgns.forall(validate(_).isRight))

  test("any failure with the last move is not king in the center is fatal"):
    val pgn = PgnStr(s"1.e4 e5 2.e3")
    assert(validate(pgn).isLeft)
