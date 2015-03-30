package lila.simul

import chess.variant.Variant
import org.joda.time.{ DateTime, Duration }
import ornicar.scalalib.Random

case class Simul(
    _id: Simul.ID,
    name: String,
    clock: SimulClock,
    nbPlayers: Int,
    players: List[SimulPlayer],
    pairings: List[SimulPairing],
    variant: Variant,
    createdAt: DateTime,
    createdBy: String) {

  def id = _id
}

object Simul {

  type ID = String

  def make(
    name: String,
    createdBy: String,
    clock: SimulClock,
    nbPlayers: Int,
    variant: chess.variant.Variant): Simul = Simul(
    _id = Random nextStringUppercase 8,
    name = name,
    clock = clock,
    createdBy = createdBy,
    createdAt = DateTime.now,
    variant = variant,
    nbPlayers = nbPlayers,
    players = Nil,
    pairings = Nil)
}
