package lila.pool

import lila.rating.{ Perf, PerfType }
import play.api.i18n.Lang
import lila.core.i18n.Translator
import lila.core.pool.PoolConfigId
import lila.core.rating.PerfKey

case class PoolConfig(
    clock: chess.Clock.Config,
    wave: PoolConfig.Wave
):
  val perfType = PerfType(chess.Speed(clock).key.into(PerfKey)) | PerfType.Classical

  val id = PoolConfig.clockToId(clock)

object PoolConfig:

  opaque type NbPlayers = Int
  object NbPlayers extends OpaqueInt[NbPlayers]

  case class Wave(every: FiniteDuration, players: NbPlayers)

  def clockToId(clock: chess.Clock.Config) = PoolConfigId(clock.show)

  import play.api.libs.json.*
  import lila.common.Json.given
  private given Lang = lila.core.i18n.defaultLang
  given (using Translator): OWrites[PoolConfig] = OWrites: p =>
    Json.obj(
      "id"   -> p.id,
      "lim"  -> p.clock.limitInMinutes,
      "inc"  -> p.clock.incrementSeconds,
      "perf" -> p.perfType.trans
    )
