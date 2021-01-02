package lila.pool

import play.api.libs.json.Json
import scala.concurrent.duration._

object PoolList {

  import PoolConfig._

  val all: List[PoolConfig] = List(
    //PoolConfig(1 ++ 0, Wave(12 seconds, 40 players))
  )

  val clockStringSet: Set[String] = all.view.map(_.clock.show) to Set

  val json = Json toJson all

  implicit private class PimpedInt(self: Int) {
    def ++(increment: Int) = chess.Clock.Config(self * 60, increment, 0, 0)
    def players            = NbPlayers(self)
  }
}
