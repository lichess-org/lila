package lila.core
package game

import _root_.chess.Color

import lila.core.id.GameId

case class Pov(game: Game, color: Color):

  export game.{ id as gameId }

  def player = game.player(color)

  def playerId = player.id

  def fullId = game.fullIdOf(color)

  def opponent = game.player(!color)

  def flip = copy(color = !color)

  def unary_! = flip

  def ref = PovRef(game.id, color)

  def withGame(g: Game)   = copy(game = g)
  def withColor(c: Color) = copy(color = c)

  lazy val isMyTurn = game.started && game.playable && game.turnColor == color

  lazy val remainingSeconds: Option[Int] =
    game.clock
      .map(c => c.remainingTime(color).roundSeconds)
      .orElse:
        game.playableCorrespondenceClock.map(_.remainingTime(color).toInt)

  def millisRemaining: Int =
    game.clock
      .map(_.remainingTime(color).millis.toInt)
      .orElse(game.correspondenceClock.map(_.remainingTime(color).toInt * 1000))
      .getOrElse(Int.MaxValue)

  def hasMoved = game.playerHasMoved(color)

  def moves = game.playerMoves(color)

  def win = game.wonBy(color)

  def loss = game.lostBy(color)

  def forecastable = game.forecastable && game.turnColor != color

  def mightClaimWin = game.forceResignable && !isMyTurn

  def sideAndStart = SideAndStart(color, game.chess.startedAtPly)

  override def toString = ref.toString

case class PovRef(gameId: GameId, color: Color):
  def unary_!           = PovRef(gameId, !color)
  override def toString = s"$gameId/${color.name}"
