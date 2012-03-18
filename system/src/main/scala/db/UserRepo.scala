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

  def updateOnlineUsernames(usernames: Iterable[String]): IO[Unit] = {
    println("yep")
    io(Unit)
  }
}
