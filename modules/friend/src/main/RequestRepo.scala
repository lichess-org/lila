package lila.friend

import lila.db.Implicits._
import lila.db.api._
import tube.requestTube

import play.api.libs.json._
import org.joda.time.DateTime

// db.friend_request.ensureIndex({friend:1})
// db.friend_request.ensureIndex({date: -1})
private[friend] object RequestRepo {

  def byId(id: String): Fu[Option[Request]] = $find byId id

  def exists(userId: String, friendId: String): Fu[Boolean] = 
    $count.exists(idQuery(userId, friendId))

  def find(userId: String, friendId: String): Fu[Option[Request]] = 
    $find byId id(userId, friendId)

  def countByFriendId(friendId: String): Fu[Int] = 
    $count(friendIdQuery(friendId))

  def findByFriendId(friendId: String): Fu[List[Request]] = 
    $find(friendIdQuery(friendId))

  def idQuery(userId: String, friendId: String) = Json.obj("_id" -> id(userId, friendId))
  def id(friendId: String, userId: String) = Request.makeId(friendId, userId)
  def friendIdQuery(friendId: String) = Json.obj("friend" -> friendId)
  def sortQuery(order: Int = -1) = Json.obj("date" -> order)
}
