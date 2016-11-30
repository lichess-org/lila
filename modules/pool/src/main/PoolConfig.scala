package lila.pool

import chess.Clock
import lila.rating.PerfType

case class PoolConfig(clock: chess.Clock) {

  val perfType = PerfType(chess.Speed(clock).key) | PerfType.Classical

  val id = PoolConfig.Id(clock.show)
}

object PoolConfig {

  case class Id(value: String) extends AnyVal

  def apply(minutes: Int, increment: Int): PoolConfig =
    PoolConfig(chess.Clock(minutes * 60, increment))
}
