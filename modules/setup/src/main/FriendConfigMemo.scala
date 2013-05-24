package lila.setup

import scala.concurrent.duration.Duration

import lila.memo.Builder

private[setup] final class FriendConfigMemo(ttl: Duration) {

  private val cache = Builder.expiry[String, FriendConfig](ttl)

  def set(gameId: String, config: FriendConfig) {
    cache.put(gameId, config)
  }

  def get(gameId: String): Option[FriendConfig] = 
    Option { cache getIfPresent gameId }
}
