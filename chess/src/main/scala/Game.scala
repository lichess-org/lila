package lila.chess

import format.PgnDump

case class Game(
    board: Board,
    player: Color,
    reversedPgnMoves: List[String] = Nil) {

  def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Game] = for {
    move ← situation.move(from, to, promotion)
  } yield {
    val newGame = copy(board = move.after, player = !player)
    val pgnMove = PgnDump.move(situation, move, newGame.situation)
    newGame.copy(reversedPgnMoves = pgnMove :: reversedPgnMoves)
  }

  lazy val situation = Situation(board, player)

  def pgnMoves = reversedPgnMoves.reverse
}

object Game {

  def apply(): Game = Game(
    board = Board(),
    player = White)
}
