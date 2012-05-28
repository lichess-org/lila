package lila
package message

import user.User

import scala.collection.mutable

final class UnreadCache(threadRepo: ThreadRepo) {

  // username, nb unread
  val cache = mutable.Map[String, Int]()

  def get(user: User): Int = get(user.id)

  def get(userId: ObjectId): Int = 
    cache.getOrElseUpdate(username.toLowerCase, {
      (threadRepo userNbUnread user).unsafePerformIO
    })

  def invalidate(user: User) {
    cache -= user.usernameCanonical
  }
}
