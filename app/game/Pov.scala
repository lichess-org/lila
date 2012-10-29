package lila
package game

import chess.Color

case class Pov(game: DbGame, color: Color) {

  def player = game player color

  def playerId = player.id

  def fullId = game fullIdOf color

  def gameId = game.id

  def opponent = game player !color

  def unary_! = Pov(game, !color)

  def isPlayerFullId(fullId: Option[String]): Boolean =
    fullId some { game.isPlayerFullId(player, _) } none false

  def ref = PovRef(game.id, color)

  def withGame(g: DbGame) = Pov(g, color)
}

object Pov {

  def apply(game: DbGame): List[Pov] = game.players.map { apply(game, _) }

  def apply(game: DbGame, player: DbPlayer) = new Pov(game, player.color)

  def apply(game: DbGame, playerId: String): Option[Pov] =
    game player playerId map { p ⇒ new Pov(game, p.color) }
}

case class PovRef(gameId: String, color: Color) {

  def unary_! = PovRef(gameId, !color)
}
