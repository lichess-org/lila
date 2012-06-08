package lila
package bookmark

import scala.collection.mutable

final class Cached(bookmarkRepo: BookmarkRepo) {

  private val gameIdsCache = mutable.Map[String, Set[String]]()

  def bookmarked(gameId: String, userId: String) =
    userGameIds(userId)(gameId)

  def count(userId: String) =
    userGameIds(userId).size

  def invalidateUserId(userId: String) = {
    gameIdsCache -= userId
  }

  private def userGameIds(userId: String): Set[String] =
    gameIdsCache.getOrElseUpdate(
      userId,
      (bookmarkRepo gameIdsByUserId userId).unsafePerformIO 
    )
}
