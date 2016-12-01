package lila.pool

import scala.concurrent.duration._

import chess.Clock
import lila.rating.PerfType

case class PoolConfig(
    clock: chess.Clock,
    wave: PoolConfig.Wave) {

  val perfType = PerfType(chess.Speed(clock).key) | PerfType.Classical

  val id = PoolConfig.Id(clock.show)
}

object PoolConfig {

  case class Id(value: String) extends AnyVal
  case class NbPlayers(value: Int) extends AnyVal

  case class Wave(every: FiniteDuration, players: NbPlayers)
}
