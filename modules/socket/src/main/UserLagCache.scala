package lila.socket

import chess.Centis
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

object UserLagCache {
  private val cache: Cache[String, Centis] = Scaffeine()
    .expireAfterWrite(2.minute)
    .build[String, Centis]

  def put(userId: String, lag: Centis) = cache.put(userId, lag)

  def get(userId: String): Option[Centis] = cache.getIfPresent(userId)

  def getLagRating(userId: String): Option[Int] = get(userId) map {
    _.centis match {
      case c if c < 15 => 4
      case c if c < 25 => 3
      case c if c < 50 => 2
      case _ => 1
    }
  }
}