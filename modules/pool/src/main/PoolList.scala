package lila.pool

import chess.Clock
import play.api.libs.json.Json

import lila.core.pool.IsClockCompatible

object PoolList:

  import PoolConfig.{ *, given }

  extension (i: Int)
    def ++(increment: Int) = Clock.Config(Clock.LimitSeconds(i * 60), Clock.IncrementSeconds(increment))
    def players = NbPlayers(i)

  val all: List[PoolConfig] = List(
    PoolConfig(1 ++ 0, Wave(12.seconds, 40.players)),
    PoolConfig(2 ++ 1, Wave(18.seconds, 30.players)),
    PoolConfig(3 ++ 0, Wave(12.seconds, 40.players)),
    PoolConfig(3 ++ 2, Wave(22.seconds, 30.players)),
    PoolConfig(5 ++ 0, Wave(14.seconds, 40.players)),
    PoolConfig(5 ++ 3, Wave(25.seconds, 26.players)),
    PoolConfig(10 ++ 0, Wave(13.seconds, 30.players)),
    PoolConfig(10 ++ 5, Wave(20.seconds, 30.players)),
    PoolConfig(15 ++ 10, Wave(30.seconds, 20.players)),
    PoolConfig(30 ++ 0, Wave(40.seconds, 20.players)),
    PoolConfig(30 ++ 20, Wave(60.seconds, 20.players))
  )

  val clockStringSet: Set[String] = all.view.map(_.clock.show) to Set

  given isClockCompatible: IsClockCompatible = IsClockCompatible: clock =>
    clockStringSet.contains(clock.show)

  def json(using lila.core.i18n.Translator) = Json.toJson(all)
