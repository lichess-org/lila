package lila.chess

import format.PgnDump
import scala.math.max

case class Game(
    board: Board = Board(),
    player: Color = White,
    pgnMoves: String = "",
    clock: Option[Clock] = None,
    deads: List[(Pos, Piece)] = Nil,
    turns: Int = 0) {

  def apply(
    orig: Pos,
    dest: Pos,
    promotion: PromotableRole = Queen): Valid[(Game, Move)] = for {
    move ← situation.move(orig, dest, promotion)
  } yield {
    val newGame = copy(
      board = move.finalizeAfter,
      player = !player,
      turns = turns + 1,
      deads = (for {
        cpos ← move.capture
        cpiece ← board(cpos)
      } yield (cpos, cpiece) :: deads) getOrElse deads
    )
    val pgnMove = PgnDump.move(situation, move, newGame.situation)
    (newGame.copy(pgnMoves = (pgnMoves + " " + pgnMove).trim), move)
  }

  def playMove(
    orig: Pos,
    dest: Pos,
    promotion: PromotableRole = Queen): Valid[Game] =
    apply(orig, dest, promotion) map (_._1)

  lazy val situation = Situation(board, player)

  def pgnMovesList = pgnMoves.split(' ').toList

  /**
   * Halfmove clock: This is the number of halfmoves
   * since the last pawn advance or capture.
   * This is used to determine if a draw
   * can be claimed under the fifty-move rule.
   */
  def halfMoveClock: Int = board.history.positionHashes.size

  /**
   * Fullmove number: The number of the full move.
   * It starts at 1, and is incremented after Black's move.
   */
  def fullMoveNumber: Int = 1 + turns / 2
}
