package lidraughts.simul

import lidraughts.game.IdGenerator

case class SimulPairing(
    player: SimulPlayer,
    gameId: String,
    status: draughts.Status,
    wins: Option[Boolean],
    hostColor: draughts.Color
) {

  def finished = status >= draughts.Status.Mate
  def ongoing = !finished

  def is(userId: String): Boolean = player is userId
  def is(other: SimulPlayer): Boolean = player is other

  def finish(s: draughts.Status, w: Option[String], t: Int) = copy(
    status = s,
    wins = w map player.is
  )

  def winnerColor = wins.map { w =>
    if (w) !hostColor else hostColor
  }
}

private[simul] object SimulPairing {

  def apply(player: SimulPlayer): SimulPairing = new SimulPairing(
    player = player,
    gameId = IdGenerator.game,
    status = draughts.Status.Created,
    wins = none,
    hostColor = draughts.White
  )
}
