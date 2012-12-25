package lila
package friend

import user.User

import scala.collection.mutable

private[friend] final class Cached(
  friendRepo: FriendRepo,
  requestRepo: RequestRepo) {

  def friendIds(userId: String): List[String] = FriendIdsCache(userId)
  def invalidateFriendIds(userId: String) { FriendIdsCache invalidate userId }

  def nbRequests(userId: String): Int = NbRequestsCache(userId)
  def invalidateNbRequests(userId: String) { NbRequestsCache invalidate userId }

  private object NbRequestsCache {

    def apply(userId: String): Int = cache.getOrElseUpdate(userId,
      (requestRepo countByFriendId userId).unsafePerformIO
    )

    def invalidate(userId: String) { cache -= userId }

    // userId => number
    private val cache = mutable.Map[String, Int]()
  }

  private object FriendIdsCache {

    def apply(userId: String): List[String] = cache.getOrElseUpdate(userId,
      (friendRepo friendUserIds userId).unsafePerformIO
    )

    def invalidate(userId: String) { cache -= userId }

    // id => name
    private val cache = mutable.Map[String, List[String]]()
  }
}
