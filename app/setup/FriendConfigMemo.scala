package lila
package setup

import memo.Builder

import scalaz.effects._

final class FriendConfigMemo(ttl: Int) {

  private val cache = Builder.expiry[String, FriendConfig](ttl)

  def set(gameId: String, config: FriendConfig): IO[Unit] = io {
    cache.put(gameId, config)
  }

  def get(gameId: String): Option[FriendConfig] = Option {
    cache getIfPresent gameId
  }
}
