package lila.lobby

import play.api.libs.json._
import reactivemongo.bson._
import org.joda.time.DateTime

// it is really a username, not a user ID
case class Message(
    user: Option[String],
    text: String,
    troll: Boolean,
    date: DateTime) {

  def render = Json.obj("u" -> user, "t" -> text)

  def isEmpty = text.isEmpty
}

object Message {

  def make(user: Option[String], text: String, troll: Boolean) = new Message(
    user = user,
    text = text,
    troll = troll,
    date = DateTime.now)

  import lila.db.Tube
  import Tube.Helpers._

  private def defaults = Json.obj("user" -> none[String])

  private[lobby] lazy val tube = Tube[Message](
    (__.json update (merge(defaults) andThen readDate('date))) andThen Json.reads[Message],
    Json.writes[Message] andThen (__.json update writeDate('date)),
    flags = Seq(_.NoId))
}
