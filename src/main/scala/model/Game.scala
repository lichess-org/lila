package lila
package model

case class Game(
    board: Board,
    player: Color) {

  def this() = this(Board.empty, White)

  val players = List(White, Black)

  lazy val actors = board actorsOf player

  lazy val moves: Map[Pos, Set[Pos]] = actors map { actor ⇒
    actor.pos -> actor.moves
  } toMap

  lazy val check: Boolean = board kingPosOf player map { king ⇒
    board actorsOf !player exists (_ threatens king)
  } getOrElse false

  lazy val checkMate: Boolean = check && moves.isEmpty

  lazy val staleMate: Boolean = !check && moves.isEmpty

  /**
   * Find all valid moves on the board from the given position for the next turn of play
   */
  def movesFrom(pos: Pos): Set[Pos] = {
    Set.empty
  }
}
