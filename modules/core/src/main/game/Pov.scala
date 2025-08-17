package lila.core
package game

import _root_.chess.Color
import scalalib.model.Seconds

import lila.core.id.GameId
import lila.core.userId.UserIdOf

case class Pov(game: Game, color: Color):

  export game.id as gameId

  def player = game.player(color)

  def playerId = player.id

  def fullId = game.fullIdOf(color)

  def opponent = game.player(!color)

  def flip = copy(color = !color)

  def unary_! = flip

  def ref = PovRef(game.id, color)

  def withGame(g: Game) = copy(game = g)
  def withColor(c: Color) = copy(color = c)

  lazy val isMyTurn = game.started && game.playable && game.turnColor == color

  lazy val remainingSeconds: Option[Seconds] =
    game.clock
      .map(c => c.remainingTime(color).roundSeconds)
      .orElse:
        game.playableCorrespondenceClock.map(_.remainingTime(color).toInt).map(Seconds(_))

  def hasMoved = game.playerHasMoved(color)

  def moves = game.playerMoves(color)

  def win = game.wonBy(color)

  def forecastable = game.forecastable && game.turnColor != color

  def mightClaimWin = game.forceResignable && !isMyTurn

  def sideAndStart = SideAndStart(color, game.chess.startedAtPly)

  override def toString = ref.toString

object Pov:
  def naturalOrientation(game: Game): Pov = Pov(game, game.naturalOrientation)
  def apply(game: Game, player: Player): Pov = Pov(game, player.color)
  def apply[U: UserIdOf](game: Game, user: U): Option[Pov] =
    game.player(user).map { apply(game, _) }

case class PovRef(gameId: GameId, color: Color):
  def unary_! = PovRef(gameId, !color)
  override def toString = s"$gameId/${color.name}"
