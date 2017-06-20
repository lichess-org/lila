package lila.socket

import chess.Centis
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

object UserLagCache {
  private val cache: Cache[String, Int] = Scaffeine()
    .expireAfterWrite(1.minute)
    .build[String, Int]

  def put(userId: String, lagTenths: Int) = cache.put(userId, lagTenths)

  def get(userId: String): Option[Centis] = {
    cache.getIfPresent(userId).map { t => Centis(10 * t) }
  }
}