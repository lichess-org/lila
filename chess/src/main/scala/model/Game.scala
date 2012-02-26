package lila.chess
package model

case class Game(
    board: Board,
    player: Color) {

  def this() = this(Board.empty, White)

  val players = List(White, Black)

  def situation = board as player
}
