package lila.pool

import scala.concurrent.duration.*

import lila.rating.{ Perf, PerfType }

case class PoolConfig(
    clock: chess.Clock.Config,
    wave: PoolConfig.Wave
):

  val perfType = PerfType(Perf.Key(chess.Speed(clock).key)) | PerfType.Classical

  val id = PoolConfig clockToId clock

object PoolConfig:

  case class Id(value: String)     extends AnyVal
  case class NbPlayers(value: Int) extends AnyVal

  case class Wave(every: FiniteDuration, players: NbPlayers)

  def clockToId(clock: chess.Clock.Config) = Id(clock.show)

  import play.api.libs.json.*
  given OWrites[PoolConfig] = OWrites { p =>
    Json.obj(
      "id"   -> p.id.value,
      "lim"  -> p.clock.limitInMinutes,
      "inc"  -> p.clock.incrementSeconds,
      "perf" -> p.perfType.trans(using lila.i18n.defaultLang)
    )
  }
