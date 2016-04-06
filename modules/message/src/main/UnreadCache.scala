package lila.message

import scala.concurrent.duration._

import lila.db.BSON._
import lila.user.User

private[message] final class UnreadCache(
    mongoCache: lila.memo.MongoCache.Builder) {

  // userId => thread IDs
  private val cache = mongoCache[String, List[String]](
    prefix = "message:unread",
    f = ThreadRepo.userUnreadIds,
    maxCapacity = 4096,
    timeToLive = 7.days,
    keyToString = identity)

  def apply(userId: String): Fu[List[String]] = cache(userId)

  def refresh(userId: String): Fu[List[String]] =
    (cache remove userId) >> apply(userId)

  def clear(userId: String) = cache remove userId
}
