package lila.friend

import org.joda.time.DateTime

import lila.user.User

case class Friend private (id: String, user1: String, user2: String, date: DateTime) {

  def users = List(user1, user2)

  def contains(userId: String): Boolean = users contains userId
  def contains(user: User): Boolean = contains(user.id)
}

object Friend {

  def makeId(u1: String, u2: String) = List(u1, u2).sorted mkString "@"

  def make(u1: String, u2: String): Friend = new Friend(
    id = makeId(u1, u2),
    user1 = u1,
    user2 = u2,
    date = DateTime.now)

  import lila.db.Tube
  import play.api.libs.json._

  private[friend] lazy val tube = Tube(
    Reads[Friend](js ⇒
      ~(for {
        obj ← js.asOpt[JsObject]
        rawFriend ← RawFriend.tube.read(obj).asOpt
        friend ← decode(rawFriend)
      } yield JsSuccess(friend): JsResult[Friend])
    ),
    Writes[Friend](friend ⇒
      RawFriend.tube.write(encode(friend)) getOrElse JsUndefined("[db] Can't write friend " + friend.id)
    )
  )

  private def encode(friend: Friend) = RawFriend(friend.id, friend.users, friend.date)

  private def decode(raw: RawFriend) = raw.users match {
    case List(u1, u2) ⇒ Friend(raw.id, u1, u2, raw.date).some
    case _ ⇒ {
      logwarn("[friend] invalid users %s" format raw)
      none
    }
  }
}

private[friend] case class RawFriend(id: String, users: List[String], date: DateTime) 

private[friend] object RawFriend {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[friend] lazy val tube = Tube[RawFriend](
    __.json update readDate('date) andThen Json.reads[RawFriend],
    Json.writes[RawFriend] andThen (__.json update writeDate('date))
  )
}
