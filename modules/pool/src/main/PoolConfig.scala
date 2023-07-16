package lila.pool

import lila.rating.{ Perf, PerfType }

case class PoolConfig(
    clock: chess.Clock.Config,
    wave: PoolConfig.Wave
):

  val perfType = PerfType(chess.Speed(clock).key into Perf.Key) | PerfType.Classical

  val id = PoolConfig clockToId clock

object PoolConfig:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  opaque type NbPlayers = Int
  object NbPlayers extends OpaqueInt[NbPlayers]

  case class Wave(every: FiniteDuration, players: NbPlayers)

  def clockToId(clock: chess.Clock.Config) = Id(clock.show)

  import play.api.libs.json.*
  import lila.common.Json.given
  given OWrites[PoolConfig] = OWrites: p =>
    Json.obj(
      "id"   -> p.id,
      "lim"  -> p.clock.limitInMinutes,
      "inc"  -> p.clock.incrementSeconds,
      "perf" -> p.perfType.trans(using lila.i18n.defaultLang)
    )
