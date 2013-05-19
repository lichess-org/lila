package lila.friend

import org.joda.time.DateTime

import lila.user.User

case class Friend(id: String, users: List[String], date: DateTime) {

  def contains(userId: String): Boolean = users contains userId
  def contains(user: User): Boolean = contains(user.id)
}

object Friend {

  def makeId(u1: String, u2: String) = List(u1, u2).sorted mkString "@" 

  def make(u1: String, u2: String): Friend = new Friend(
    id = makeId(u1, u2),
    users = List(u1, u2), 
    date = DateTime.now)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[friend] lazy val tube = Tube[Friend](
    __.json update readDate('date) andThen Json.reads[Friend],
    Json.writes[Friend] andThen (__.json update writeDate('date))
  )
}
