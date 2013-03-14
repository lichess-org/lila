package lila.app
package message

import user.User

import scala.collection.mutable

final class UnreadCache(threadRepo: ThreadRepo) {

  // userId, nb unread
  val cache = mutable.Map[String, Int]()

  def get(user: User): Int = get(user.id)

  def get(userId: String): Int = 
    cache.getOrElseUpdate(userId.toLowerCase, {
      (threadRepo userNbUnread userId).unsafePerformIO
    })

  def refresh(userId: String): Int = {
    cache -= userId
    get(userId)
  }
}
