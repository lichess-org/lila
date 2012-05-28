package lila
package user

import scala.collection.mutable

final class Cached(userRepo: UserRepo) {

  // id => username
  val usernameCache = mutable.Map[String, Option[String]]()

  def username(userId: String) =
    usernameCache.getOrElseUpdate(
      userId.toLowerCase,
      (userRepo username userId).unsafePerformIO 
    )

  def usernameOrAnonymous(userId: String) = username(userId) | User.anonymous
}
