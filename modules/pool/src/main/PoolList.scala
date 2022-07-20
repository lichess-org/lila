package lila.pool

import play.api.libs.json.Json

object PoolList {

  import PoolConfig._

  val all: List[PoolConfig] = List(
    // PoolConfig(1 ++ 0, Wave(12 seconds, 40 players))
  )

  val clockStringSet: Set[String] = all.view.map(_.clock.show) to Set

  val json = Json toJson all

  implicit private class PimpedInt(self: Int) {
    def ++(increment: Int) = shogi.Clock.Config(self * 60, increment, 0, 0)
    def players            = NbPlayers(self)
  }
}
