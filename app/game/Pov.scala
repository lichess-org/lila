package lila
package game

import chess.Color

case class Pov(game: DbGame, color: Color) {

  def player = game player color

  def playerId = player.id

  def opponent = game player !color

  def isPlayerFullId(fullId: Option[String]): Boolean =
    fullId some { game.isPlayerFullId(player, _) } none false

  def ref = PovRef(game.id, color)
}

object Pov {

  def apply(game: DbGame, player: DbPlayer) = new Pov(game, player.color)

  def apply(game: DbGame, playerId: String): Option[Pov] =
    game player playerId map { p ⇒ new Pov(game, p.color) }
}

case class PovRef(gameId: String, color: Color)
