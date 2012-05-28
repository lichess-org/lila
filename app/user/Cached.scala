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

  def usernameOrAnonymous(userId: String): String = 
    username(userId) | User.anonymous

  def usernameOrAnonymous(userId: Option[String]): String = 
    userId.fold(usernameOrAnonymous, User.anonymous)
}
