package lila.pool

import scala.concurrent.duration._

import lila.rating.PerfType

case class PoolConfig(
    clock: chess.Clock.Config,
    wave: PoolConfig.Wave
) {

  val perfType = PerfType(chess.Speed(clock).key) | PerfType.Classical

  val id = PoolConfig clockToId clock
}

object PoolConfig {

  case class Id(value: String) extends AnyVal
  case class NbPlayers(value: Int) extends AnyVal

  case class Wave(every: FiniteDuration, players: NbPlayers)

  def clockToId(clock: chess.Clock.Config) = Id(clock.show)
}
