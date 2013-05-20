package lila.friend

import lila.db.Implicits._
import lila.db.api._
import tube.friendTube

import play.api.libs.json._
import org.joda.time.DateTime

private[friend] object FriendRepo {

  def byId(id: ID): Fu[Option[Friend]] = $find byId id

  def byUsers(u1: ID, u2: ID): Fu[Option[Friend]] = $find byId Friend.makeId(u1, u2)

  def friendUserIds(userId: ID): Fu[List[ID]] = 
    $primitive(Json.obj("users" -> userId), "users")(_.asOpt[List[ID]]) map {
      _.flatten filterNot (userId ==)
    }

  def add(u1: ID, u2: ID): Funit = $insert(Friend.make(u1, u2).pp)

  def remove(u1: ID, u2: ID): Funit = $remove byId Friend.makeId(u1, u2)
}
