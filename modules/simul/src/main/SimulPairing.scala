package lila.simul

import lila.game.{ PovRef, IdGenerator }

case class SimulPairing(
    gameId: String,
    status: chess.Status,
    user: String,
    wins: Option[Boolean],
    turns: Option[Int]) {

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def finish(s: chess.Status, w: Option[String], t: Int) = copy(
    status = s,
    wins = w map (user==),
    turns = t.some)
}

private[simul] object SimulPairing {

  def apply(user: String): SimulPairing = new SimulPairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user = user,
    wins = none,
    turns = none)
}
