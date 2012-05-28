package lila
package message

import user.User

import scala.collection.mutable

final class UnreadCache(threadRepo: ThreadRepo) {

  // username, nb unread
  val cache = mutable.Map[String, Int]()

  def get(user: User): Int = get(user.id)

  def get(username: String): Int = 
    cache.getOrElseUpdate(username.toLowerCase, {
      (threadRepo userNbUnread username).unsafePerformIO
    })

  def refresh(user: User): Int = {
    cache -= user.id
    get(user)
  }
}
