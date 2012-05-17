package lila
package user

import scala.collection.mutable

final class Cached(userRepo: UserRepo) {

  val usernameCache = mutable.Map[String, String]()

  def username(userId: String) =
    usernameCache.getOrElseUpdate(
      userId,
      (userRepo username userId).unsafePerformIO | "Anonymous"
    )
}
