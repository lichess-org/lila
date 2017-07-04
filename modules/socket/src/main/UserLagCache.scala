package lila.socket

import chess.Centis
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

object UserLagCache {
  private val cache: Cache[String, Int] = Scaffeine()
    .expireAfterWrite(5.minute)
    .build[String, Int]

  // Store 1/50th of second to get most values into the java int cache
  // and reduce memory pressure.
  def put(userId: String, lag: Centis) = {
    val newLag = lag.centis >> 1
    cache.put(userId, cache.getIfPresent(userId).fold(newLag) {
      i => (i + newLag) >> 1
    })
  }

  def get(userId: String): Option[Centis] = cache.getIfPresent(userId) map {
    i => Centis(i << 1)
  }

  def getLagRating(userId: String): Option[Int] = cache.getIfPresent(userId) map {
    case i if i < 8 => 4
    case i if i < 14 => 3
    case i if i < 22 => 2
    case _ => 1
  }
}