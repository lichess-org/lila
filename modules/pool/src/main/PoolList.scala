package lila.pool

import scala.concurrent.duration._

private object PoolList {

  import PoolConfig._

  val all: List[PoolConfig] = List(
    PoolConfig(1 ++ 0, Wave(12 seconds, 20 players)),
    PoolConfig(2 ++ 1, Wave(15 seconds, 20 players)),
    PoolConfig(3 ++ 0, Wave(15 seconds, 30 players)),
    PoolConfig(3 ++ 2, Wave(15 seconds, 20 players)),
    PoolConfig(5 ++ 0, Wave(15 seconds, 30 players)),
    PoolConfig(5 ++ 3, Wave(20 seconds, 20 players)),
    PoolConfig(10 ++ 0, Wave(30 seconds, 20 players)),
    PoolConfig(15 ++ 15, Wave(120 seconds, 16 players))
  )

  private implicit final class PimpedInt(self: Int) {
    def ++(increment: Int) = chess.Clock(self * 60, increment)
    def players = NbPlayers(self)
  }
}
