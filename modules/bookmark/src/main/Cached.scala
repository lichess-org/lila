package lila.bookmark

import scala.concurrent.duration._
import lila.memo.MixedCache

private[bookmark] final class Cached {

  private[bookmark] val gameIdsCache = MixedCache[String, Set[String]](
    (userId: String) => BookmarkRepo gameIdsByUserId userId map (_.toSet),
    timeToLive = 1 day,
    default = _ => Set.empty)

  def gameIds(userId: String) = gameIdsCache get userId

  def bookmarked(gameId: String, userId: String): Boolean =
    gameIds(userId) contains gameId

  def count(userId: String): Int = gameIds(userId).size
}
