package lila.bookmark

import lila.memo.AsyncCache

private[bookmark] final class Cached {

  val gameIds = AsyncCache(
    (userId: String) => BookmarkRepo gameIdsByUserId userId map (_.toSet),
    maxCapacity = 50000)

  def bookmarked(gameId: String, userId: String): Fu[Boolean] =
    gameIds(userId) map (_ contains gameId)

  def count(userId: String): Fu[Int] =
    gameIds(userId) map (_.size)
}
