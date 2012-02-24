package lila
package model

case class Game(
    board: Board,
    nextPlayer: Color) {

  def this() = this(Board.empty, White)

  val players = List(White, Black)

  def moves: Map[Pos, Set[Pos]] = board.actors collect {
    case (pos, actor) if actor is nextPlayer ⇒ pos -> actor.moves
  } toMap

  /**
   * Find all valid moves on the board from the given position for the next turn of play
   */
  def movesFrom(pos: Pos): Set[Pos] = {
    Set.empty
  }
}
