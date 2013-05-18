package lila.game

import chess.Color

abstract class Handler(gameId: String) {

  protected def blocking[A](playerId: String)(op: Pov ⇒ Fu[A]): A = {
    GameRepo pov PlayerRef(gameId, playerId) flatten "No such game" flatMap op
  }.await

  protected def blocking[A](color: Color)(op: Pov ⇒ Fu[A]): A = {
    GameRepo pov PovRef(gameId, color) flatten "No such game" flatMap op
  }.await

  protected def blocking[A](op: Game ⇒ Fu[A]): A = {
    GameRepo game gameId flatten "No such game" flatMap op
  }.await
}
