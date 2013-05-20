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
    $count.exists($select(Request.makeId(userId, friendId)))

  def byUsers(u1: ID, u2: ID): Fu[Option[Request]] = $find byId Request.makeId(u1, u2)

  def countByFriendId(friendId: String): Fu[Int] = 
    $count(friendIdQuery(friendId))

  def findByFriendId(friendId: String): Fu[List[Request]] = 
    $find(friendIdQuery(friendId))

  def friendIdQuery(friendId: String) = Json.obj("friend" -> friendId)
}
