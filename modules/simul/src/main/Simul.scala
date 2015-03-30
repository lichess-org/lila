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
    variants: List[Variant],
    createdAt: DateTime,
    createdBy: String,
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime]) {

  def id = _id
}

object Simul {

  type ID = String

  def make(
    name: String,
    createdBy: String,
    clock: SimulClock,
    nbPlayers: Int,
    variants: List[Variant]): Simul = Simul(
    _id = Random nextStringUppercase 8,
    name = name,
    clock = clock,
    createdBy = createdBy,
    createdAt = DateTime.now,
    variants = variants,
    nbPlayers = nbPlayers,
    players = Nil,
    pairings = Nil,
    startedAt = none,
    finishedAt = none)
}
