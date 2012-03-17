package lila.system

import org.sedis._
import redis.clients.jedis._
import Dress._

final class VersionCache(pool: Pool) {

  def get(gameId: String, color: Color): Option[Int] =
    pool.withClient { client ⇒
      client get key(gameId, color) flatMap parseIntOption
    }

  def key(gameId: String, color: Color) = gameId + ":" + color.letter + ":v"
}
