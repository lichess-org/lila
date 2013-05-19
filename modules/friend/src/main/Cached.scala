package lila.friend

import lila.user.User

import scala.collection.mutable

private[friend] final class Cached {

  def friendIds(userId: String): List[String] = FriendIdsCache(userId)
  def invalidateFriendIds(userId: String) { FriendIdsCache invalidate userId }

  def nbRequests(userId: String): Int = NbRequestsCache(userId)
  def invalidateNbRequests(userId: String) { NbRequestsCache invalidate userId }

  private object NbRequestsCache {

    //TODO fix that crap
    def apply(userId: String): Int = cache.getOrElseUpdate(userId,
      (RequestRepo countByFriendId userId).await
    )

    def invalidate(userId: String) { cache -= userId }

    // userId => number
    private val cache = mutable.Map[String, Int]()
  }

  private object FriendIdsCache {

    def apply(userId: String): List[String] = cache.getOrElseUpdate(userId,
      (FriendRepo friendUserIds userId).await
    )

    def invalidate(userId: String) { cache -= userId }

    // id => name
    private val cache = mutable.Map[String, List[String]]()
  }
}
