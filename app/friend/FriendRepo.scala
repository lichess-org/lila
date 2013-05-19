package lila
package friend

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import org.joda.time.{ DateTime, Period }
import org.scala_tools.time.Imports._

import user.User

private[friend] final class FriendRepo(collection: MongoCollection)
    extends SalatDAO[Friend, String](collection) {

  def byId(id: String): IO[Option[Friend]] = io { findOneById(id) }

  def friendUserIds(userId: String): IO[List[String]] = io {
    collection.find(
      DBObject("users" -> userId),
      DBObject("users" -> true)
    ).map(_.getAs[List[String]]("users")).flatten.toList.flatten.filterNot(userId ==)
  }

  def add(u1: String, u2: String) = io {
    insert(Friend(u1, u2))
  }

  def selectId(id: String): DBObject = DBObject("_id" -> id)
  def selectId(friend: Friend): DBObject = selectId(friend.id)
}
