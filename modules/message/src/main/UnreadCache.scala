package lila.message

import spray.caching.{ LruCache, Cache }

import lila.user.User

private[message] final class UnreadCache {

  // userId => thread IDs
  private val cache: Cache[List[String]] = LruCache(maxCapacity = 99999)

  def apply(userId: String): Fu[List[String]] =
    cache(userId)(ThreadRepo userUnreadIds userId)

  def refresh(userId: String): Fu[List[String]] =
    (cache remove userId).fold(apply(userId))(_ >> apply(userId))
}
