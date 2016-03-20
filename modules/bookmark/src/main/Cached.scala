package lila.bookmark

import lila.memo.MixedCache
import scala.concurrent.duration._

private[bookmark] final class Cached {

  private[bookmark] val gameIdsCache = MixedCache[String, Set[String]](
    BookmarkRepo.gameIdsByUserId,
    timeToLive = 1 day,
    default = _ => Set.empty,
    logger = lila.log("bookmark"))

  def gameIds(userId: String) = gameIdsCache get userId

  def bookmarked(gameId: String, userId: String): Boolean =
    gameIds(userId) contains gameId

  def count(userId: String): Int = gameIds(userId).size
}
