package lila.friend

import lila.db.Implicits._
import lila.db.api._
import tube.friendTube

import play.api.libs.json._
import org.joda.time.DateTime

private[friend] object FriendRepo {

  def byId(id: String): Fu[Option[Friend]] = $find byId id

  def friendUserIds(userId: String): Fu[List[String]] = 
    $primitive(Json.obj("users" -> userId), "users")(_.asOpt[List[String]]) map {
      _.flatten filterNot (userId ==)
    }

  def add(u1: String, u2: String): Funit = $insert(Friend.make(u1, u2))
}
