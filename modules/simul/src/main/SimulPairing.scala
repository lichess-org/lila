package lila.simul

import lila.game.{ PovRef, IdGenerator }

case class SimulPairing(
    player: SimulPlayer,
    gameId: String,
    status: chess.Status,
    wins: Option[Boolean],
    turns: Option[Int]) {

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def is(userId: String): Boolean = player is userId
  def is(other: SimulPlayer): Boolean = player is other

  def finish(s: chess.Status, w: Option[String], t: Int) = copy(
    status = s,
    wins = w map player.is,
    turns = t.some)
}

private[simul] object SimulPairing {

  def apply(player: SimulPlayer): SimulPairing = new SimulPairing(
    player = player,
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    wins = none,
    turns = none)
}
