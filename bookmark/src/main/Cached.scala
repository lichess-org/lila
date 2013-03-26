package lila.bookmark

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }

import play.api.libs.concurrent.Execution.Implicits._

private[bookmark] final class Cached(bookmarkRepo: BookmarkRepo) {

  def bookmarked(gameId: String, userId: String): Fu[Boolean] =
    userGameIds(userId) map (_ contains gameId)

  def count(userId: String): Fu[Int] =
    userGameIds(userId) map (_.size)

  def invalidateUserId(userId: String) {
    gameIdsCache.remove(userId)
  }

  private def userGameIds(userId: String): Fu[Set[String]] =
    gameIdsCache.fromFuture(userId.toLowerCase) {
      bookmarkRepo gameIdsByUserId userId.toLowerCase map (_.toSet)
    }

  private val gameIdsCache: Cache[Set[String]] = LruCache(maxCapacity = 99999)
}
