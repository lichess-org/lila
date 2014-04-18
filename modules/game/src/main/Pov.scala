package lila.game

import chess.Color

case class Pov(game: Game, color: Color) {

  def player = game player color

  def playerId = player.id

  def fullId = game fullIdOf color

  def gameId = game.id

  def opponent = game player !color

  def isFirstPlayer = game.firstPlayer.color == color

  def unary_! = Pov(game, !color)

  def isPlayerFullId(fullId: Option[String]): Boolean =
    fullId ?? { game.isPlayerFullId(player, _) }

  def ref = PovRef(game.id, color)

  def withGame(g: Game) = Pov(g, color)

  override def toString = ref.toString
}

object Pov {

  def apply(game: Game): List[Pov] = game.players.map { apply(game, _) }

  def first(game: Game) = apply(game, game.firstPlayer)
  def second(game: Game) = apply(game, game.secondPlayer)
  def white(game: Game) = apply(game, game.whitePlayer)
  def black(game: Game) = apply(game, game.blackPlayer)

  def apply(game: Game, player: Player) = new Pov(game, player.color)

  def apply(game: Game, playerId: String): Option[Pov] =
    game player playerId map { apply(game, _) }

  def apply(game: Game, user: lila.user.User): Option[Pov] =
    game player user map { apply(game, _) }
}

case class PovRef(gameId: String, color: Color) {

  def unary_! = PovRef(gameId, !color)
}

case class PlayerRef(gameId: String, playerId: String)

object PlayerRef {

  def apply(fullId: String): PlayerRef = PlayerRef(Game takeGameId fullId, Game takePlayerId fullId)
}
