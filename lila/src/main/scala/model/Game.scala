package lila
package model

case class Game(
  board: Board,
  history: List[(Pos, Pos)],
  nextMove: Color) {

  def this() = this(Board(), Nil, White)

  val players = List(White, Black)

  def possibleMoves: Map[Pos, Set[Pos]] = {
    val default: Map[Pos, Set[Pos]] = Map.empty
    board.pieces.foldLeft(default) { (moves, boardElement) =>
      val (pos, piece) = boardElement
      if (nextMove.equals(piece.color)) {
        val viableMoves = movesFrom(pos)
        if (viableMoves.isEmpty) {
          moves
        } else {
          moves + ((pos, viableMoves))
        }
      } else {
        moves
      }
    }
  }

  /**
   * Find all valid moves on the board from the given position for the next turn of play
   */
  def movesFrom(pos: Pos): Set[Pos] = {
    Set.empty
  }
}
