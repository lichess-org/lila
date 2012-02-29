package lila.chess

import format.PgnDump

case class Game(
    board: Board,
    player: Color,
    pgnMoves: String = "") {

  def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Game] = for {
    move ← situation.move(from, to, promotion)
  } yield {
    val newGame = copy(
      board = move.afterWithPositionHashesUpdated,
      player = !player
    )
    val pgnMove = PgnDump.move(situation, move, newGame.situation)
    newGame.copy(pgnMoves = (pgnMoves + " " + pgnMove).trim)
  }

  lazy val situation = Situation(board, player)

  def pgnMovesList = pgnMoves.split(' ').toList
}

object Game {

  def apply(): Game = Game(
    board = Board(),
    player = White)
}
