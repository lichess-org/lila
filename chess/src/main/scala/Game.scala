package lila.chess

import format.PgnDump

case class Game(
    board: Board,
    player: Color,
    reversedPgnMoves: List[String] = Nil) {

  def this() = this(Board.empty, White)

  def playMoves(moves: (Pos, Pos)*): Valid[Game] =
    moves.foldLeft(success(this): Valid[Game]) { (sit, move) ⇒
      sit flatMap { s ⇒ s.playMove(move._1, move._2) }
    }

  def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Game] = for {
    move ← situation.move(from, to, promotion)
  } yield copy(
    board = move.after,
    player = !player,
    reversedPgnMoves = (PgnDump move move) :: reversedPgnMoves
  )

  val players = List(White, Black)

  def situation = board as player

  def as(c: Color) = copy(player = c)

  def pgnMoves = reversedPgnMoves.reverse
}

object Game {

  def apply(): Game = Game(
    board = Board(),
    player = White)
}
