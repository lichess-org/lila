package lila
package user

import scala.collection.mutable
import com.mongodb.casbah.Imports.ObjectId

final class Cached(userRepo: UserRepo) {

  // idString => username|Anonymous
  val usernameCache = mutable.Map[String, String]()

  // username => Option[ObjectId]
  val idCache = mutable.Map[String, Option[ObjectId]]()

  def username(userId: String) =
    usernameCache.getOrElseUpdate(
      userId,
      (userRepo username userId).unsafePerformIO | "Anonymous"
    )

  def id(username: String): Option[ObjectId] =
    idCache.getOrElseUpdate(
      username.toLowerCase,
      (userRepo id username).unsafePerformIO 
    )
}
