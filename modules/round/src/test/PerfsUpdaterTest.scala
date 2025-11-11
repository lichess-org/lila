package lila.round

import chess.*
import chess.format.Fen
import chess.variant.*
import lila.rating.Glicko.*

import PerfsUpdater.withCalculator
import Rematcher.returnChessGame

class PerfsUpdaterTest extends munit.FunSuite:

  test("chess960 && shouldRepeatChessPosition"):
    val originalBoard = Position(Board.fromMap(Chess960.initialPieces(317)), Chess960, Color.White)
    val x = returnChessGame(Chess960, none, Fen.write(originalBoard).some, true)
    assertEquals(withCalculator(x.variant), calculator)

  test("FromPosition with custom FEN"):
    val originalFen = Fen.Full("rqbnknrb/pppppppp/8/8/8/8/PPPPPPPP/RQBNKNRB w Qq - 0 1")
    val x = returnChessGame(FromPosition, none, originalFen.some, false)
    assertEquals(withCalculator(x.variant), calculator)

  test("FromPosition with no FEN"):
    val x = returnChessGame(FromPosition, none, none, false)
    assertEquals(withCalculator(x.variant), calculator)

  test("Crazyhouse"):
    val x = returnChessGame(Crazyhouse, none, none, false)
    assertEquals(withCalculator(x.variant), calculatorWithCrazyhouseAdvantage)

  test("Standard with no FEN"):
    val x = returnChessGame(Standard, none, none, false)
    assertEquals(withCalculator(x.variant), calculatorWithAdvantage)

  test("other variants"):
    Variant.list.all
      .filter(_ != Chess960)
      .filter(_ != Crazyhouse)
      .filter(_ != Standard)
      .foreach: variant =>
        val x = returnChessGame(variant, none, none, false)
        assertEquals(withCalculator(x.variant), calculator)
