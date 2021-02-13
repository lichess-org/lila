package lila.simul

import lila.game.IdGenerator

final case class SimulPairing(
    player: SimulPlayer,
    gameId: String,
    status: chess.Status,
    wins: Option[Boolean],
    hostColor: chess.Color
) {

  def finished = status >= chess.Status.Aborted
  def ongoing  = !finished

  def is(userId: String): Boolean     = player is userId
  def is(other: SimulPlayer): Boolean = player is other

  def finish(s: chess.Status, w: Option[String]) =
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
      status = chess.Status.Created,
      wins = none,
      hostColor = chess.White
    )
}
