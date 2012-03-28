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

  def user(userId: String): IO[User] = user(new ObjectId(userId))

  def user(userId: ObjectId): IO[User] = io {
    findOneByID(userId) err "No user found for id " + userId
  }

  def setElo(userId: ObjectId, elo: Int): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> userId),
      $set ("elo" -> elo))
  }

  def incNbGames(userId: String, rated: Boolean): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> new ObjectId(userId)),
      if (rated) $inc ("nbGames" -> 1, "nbRatedGames" -> 1)
      else $inc ("nbGames" -> 1))
  }

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
