package lila.round

import chess.*
import chess.format.Fen
import chess.variant.*

import Rematcher.*

class RematcherTest extends munit.FunSuite:

  test("chess960 && shouldRepeatChessPosition"):
    val originalSituation = Situation(Board(Chess960.pieces(317), Chess960), Color.White)
    val x                 = returnChessGame(Chess960, none, Fen.write(originalSituation).some, true)
    assertEquals(x.situation, originalSituation)

  // this test is flaky because we the new generated position could be the same as the original
  test("chess960 && not shouldRepeatChessPosition".flaky):
    val originalSituation = Situation(Board(Chess960.pieces(317), Chess960), Color.White)
    val x                 = returnChessGame(Chess960, none, Fen.write(originalSituation).some, false)
    assertNotEquals(x.situation, originalSituation)

  test("FromPosition with custom Fen"):
    val originalFen       = Fen.Full("rqbnknrb/pppppppp/8/8/8/8/PPPPPPPP/RQBNKNRB w Qq - 0 1")
    val originalSituation = Fen.read(FromPosition, originalFen)
    val x                 = returnChessGame(FromPosition, none, originalFen.some, false)
    assertEquals(x.situation.some, originalSituation)

  test("FromPosition with no Fen"):
    val x = returnChessGame(FromPosition, none, none, false)
    assertEquals(x.situation.variant, FromPosition)
    assertEquals(x.situation.board.pieces, Standard.pieces)

  test("all variants except Chess960"):
    Variant.list.all
      .filter(_ != Chess960)
      .foreach: variant =>
        val x = returnChessGame(variant, none, none, false)
        assertEquals(x.situation, Situation(variant))
