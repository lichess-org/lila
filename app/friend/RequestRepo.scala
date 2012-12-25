package lila
package friend

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._
import org.joda.time.DateTime

// db.friend_request.ensureIndex({friend:1})
// db.friend_request.ensureIndex({date: -1})
private[friend] final class RequestRepo(collection: MongoCollection)
    extends SalatDAO[Request, String](collection) {

  def byId(id: String): IO[Option[Request]] = io { findOneById(id) }

  def exists(userId: String, friendId: String): IO[Boolean] = io {
    collection.find(idQuery(userId, friendId)).limit(1).size != 0
  }

  def find(userId: String, friendId: String): IO[Option[Request]] = io {
    findOneById(id(userId, friendId))
  }

  def countByFriendId(friendId: String): IO[Int] = io {
    count(friendIdQuery(friendId)).toInt
  }

  def findByFriendId(friendId: String): IO[List[Request]] = io {
    find(friendIdQuery(friendId)).toList
  }

  def idQuery(userId: String, friendId: String) = DBObject("_id" -> id(userId, friendId))
  def idQuery(id: String) = DBObject("_id" -> id)
  def id(friendId: String, userId: String) = Request.makeId(friendId, userId)
  def friendIdQuery(friendId: String) = DBObject("friend" -> friendId)
  def sortQuery(order: Int = -1) = DBObject("date" -> order)

  def add(request: Request): IO[Unit] = io { insert(request) }

  def remove(id: String): IO[Unit] = io { remove(idQuery(id)) }
}
