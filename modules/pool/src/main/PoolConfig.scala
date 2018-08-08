package lidraughts.pool

import scala.concurrent.duration._

import lidraughts.rating.PerfType

case class PoolConfig(
    clock: draughts.Clock.Config,
    wave: PoolConfig.Wave
) {

  val perfType = PerfType(draughts.Speed(clock).key) | PerfType.Classical

  val id = PoolConfig clockToId clock
}

object PoolConfig {

  case class Id(value: String) extends AnyVal
  case class NbPlayers(value: Int) extends AnyVal

  case class Wave(every: FiniteDuration, players: NbPlayers)

  def clockToId(clock: draughts.Clock.Config) = Id(clock.show)
}
