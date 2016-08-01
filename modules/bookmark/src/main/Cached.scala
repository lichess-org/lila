package lila.bookmark

import lila.memo.{ AsyncCache, MixedCache }
import scala.concurrent.duration._

private[bookmark] final class Cached {

  private[bookmark] val gameIdsCache = AsyncCache[String, Set[String]](
    BookmarkRepo.gameIdsByUserId,
    timeToLive = 1 hour)

  private[bookmark] val gameIdsMixedCache = MixedCache.fromAsync[String, Set[String]](
    gameIdsCache,
    timeToLive = 1 hour,
    default = _ => Set.empty,
    logger = lila.log("bookmark"))

  def gameIds(userId: String): Fu[Set[String]] = gameIdsCache(userId)

  def bookmarked(gameId: String, userId: String): Boolean =
    gameIdsMixedCache get userId contains gameId

  def count(userId: String): Fu[Int] = gameIdsCache(userId).map(_.size)

  private[bookmark] def invalidate(userId: String): Funit =
    gameIdsMixedCache invalidate userId
}
