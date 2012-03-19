package lila.system
package db

import model.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class UserRepo(collection: MongoCollection)
    extends SalatDAO[User, ObjectId](collection) {

  def updateOnlineUsernames(usernames: Iterable[String]): IO[Unit] = io {
    val names = usernames map (_.toLowerCase)
    collection.update(
      ("usernameCanonical" $nin names) ++ ("isOnline" -> true),
      $set ("isOnline" -> false),
      upsert = false,
      multi = true)
    collection.update(
      ("usernameCanonical" $in names) ++ ("isOnline" -> false),
      $set ("isOnline" -> true),
      upsert = false,
      multi = true)
  }
}
