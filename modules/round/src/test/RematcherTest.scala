package lila.round

import chess.*
import chess.format.Fen
import chess.variant.*

import Rematcher.*

class RematcherTest extends munit.FunSuite:

  test("chess960 && shouldRepeatChessPosition"):
    val originalBoard = Position(Chess960.pieces(317), Chess960, Color.White.some)
    val x             = returnChessGame(Chess960, none, Fen.write(originalBoard).some, true)
    assertEquals(x.board, originalBoard)

  // this test is flaky because we the new generated position could be the same as the original
  test("chess960 && not shouldRepeatChessPosition".flaky):
    val originalBoard = Position(Chess960.pieces(317), Chess960, Color.White.some)
    val x             = returnChessGame(Chess960, none, Fen.write(originalBoard).some, false)
    assertNotEquals(x.board, originalBoard)

  test("FromPosition with custom Fen"):
    val originalFen   = Fen.Full("rqbnknrb/pppppppp/8/8/8/8/PPPPPPPP/RQBNKNRB w Qq - 0 1")
    val originalBoard = Fen.read(FromPosition, originalFen)
    val x             = returnChessGame(FromPosition, none, originalFen.some, false)
    assertEquals(x.board.some, originalBoard)

  test("FromPosition with no Fen"):
    val x = returnChessGame(FromPosition, none, none, false)
    assertEquals(x.board.variant, FromPosition)
    assertEquals(x.board.pieces, Standard.pieces)

  test("all variants except Chess960"):
    Variant.list.all
      .filter(_ != Chess960)
      .foreach: variant =>
        val x = returnChessGame(variant, none, none, false)
        assertEquals(x.board, Position(variant))
