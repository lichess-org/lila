package lila.simul

import lila.game.IdGenerator

final case class SimulPairing(
    player: SimulPlayer,
    gameId: String,
    status: shogi.Status,
    wins: Option[Boolean],
    hostColor: shogi.Color
) {

  def finished = status >= shogi.Status.Aborted
  def ongoing  = !finished

  def is(userId: String): Boolean     = player is userId
  def is(other: SimulPlayer): Boolean = player is other

  def finish(s: shogi.Status, w: Option[String]) =
    copy(
      status = s,
      wins = w map player.is
    )

  def winnerColor =
    wins.map { w =>
      if (w) !hostColor else hostColor
    }
}

private[simul] object SimulPairing {

  def apply(player: SimulPlayer): SimulPairing =
    new SimulPairing(
      player = player,
      gameId = IdGenerator.uncheckedGame,
      status = shogi.Status.Created,
      wins = none,
      hostColor = shogi.Sente
    )
}
