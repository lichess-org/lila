package lila.pool

import scala.concurrent.duration._

object PoolList {

  import PoolConfig._

  val all: List[PoolConfig] = List(
    PoolConfig(1 ++ 0, Wave(13 seconds, 24 players)),
    PoolConfig(2 ++ 1, Wave(18 seconds, 20 players)),
    PoolConfig(3 ++ 0, Wave(12 seconds, 30 players)),
    PoolConfig(3 ++ 2, Wave(22 seconds, 20 players)),
    PoolConfig(5 ++ 0, Wave(10 seconds, 30 players)),
    PoolConfig(5 ++ 3, Wave(25 seconds, 20 players)),
    PoolConfig(10 ++ 0, Wave(13 seconds, 24 players)),
    PoolConfig(15 ++ 15, Wave(60 seconds, 16 players))
  )

  val clockStringSet: Set[String] = all.map(_.clock.show)(scala.collection.breakOut)

  private implicit class PimpedInt(self: Int) {
    def ++(increment: Int) = chess.Clock.Config(self * 60, increment)
    def players = NbPlayers(self)
  }
}
