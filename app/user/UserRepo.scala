package lila
package user

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.DBRef
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._
import com.roundeights.hasher.Implicits._

class UserRepo(
    collection: MongoCollection,
    val dbRef: User ⇒ DBRef) extends SalatDAO[User, ObjectId](collection) {

  private val enabledQuery = DBObject("enabled" -> true)

  def user(userId: String): IO[Option[User]] = user(new ObjectId(userId))

  def user(userId: ObjectId): IO[Option[User]] = io {
    findOneByID(userId)
  }

  def username(userId: String): IO[Option[String]] = io {
    primitiveProjection[String](
      DBObject("_id" -> new ObjectId(userId)),
      "username")
  }

  def byUsername(username: String): IO[Option[User]] = io {
    findOne(
      DBObject("usernameCanonical" -> username.toLowerCase)
    )
  }

  def setElo(userId: ObjectId, elo: Int): IO[Unit] = io {
    collection.update(
      idSelector(userId),
      $set("elo" -> elo))
  }

  def setEngine(userId: ObjectId): IO[Unit] = io {
    collection.update(
      idSelector(userId),
      $set("engine" -> true))
  }

  def incNbGames(userId: String, rated: Boolean): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> new ObjectId(userId)),
      if (rated) $inc("nbGames" -> 1, "nbRatedGames" -> 1)
      else $inc("nbGames" -> 1))
  }

  val averageElo: IO[Float] = io {
    val elos = find(DBObject()).toList map (_.elo)
    elos.sum / elos.size.toFloat
  }

  def toggleChatBan(user: User): IO[Unit] = io {
    collection.update(
      idSelector(user),
      $set("isChatBan" -> !user.isChatBan))
  }

  def saveSetting(user: User, key: String, value: String) = io {
    collection.update(
      idSelector(user),
      $set(("settings." + key) -> value))
  }

  def authenticate(username: String, password: String): IO[Option[User]] =
    byUsername(username) map { userOption ⇒
      userOption filter { u ⇒ u.password == hash(password, u.salt) }
    }

  val countEnabled: IO[Int] = io { count().toInt }

  def usernamesLike(username: String): IO[List[String]] = io {
    val regex = "^" + username.toLowerCase + ".*$"
    collection.find(
      DBObject("usernameCanonical" -> regex.r),
      DBObject("username" -> 1))
      .sort(DBObject("usernameCanonical" -> 1))
      .limit(10)
      .toList
      .map(_.expand[String]("username"))
      .flatten
  }

  private def idSelector(user: User) = DBObject("_id" -> user.id)

  private def idSelector(id: ObjectId) = DBObject("_id" -> id)

  private def hash(pass: String, salt: String): String =
    "%s{%s}".format(pass, salt).sha1
}
