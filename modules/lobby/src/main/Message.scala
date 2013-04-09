package lila.lobby

import play.api.libs.json._
import reactivemongo.bson._
import org.joda.time.DateTime

private[lobby] case class Message(
    userId: String,
    text: String,
    date: DateTime) {

  def render = Json.obj("txt" -> text, "u" -> userId)

  def isEmpty = text.isEmpty
}

object Message {

  def make(userId: String, text: String) = new Message(
    userId = userId,
    text = text,
    date = DateTime.now)

  import lila.db.Tube
  import Tube.Helpers._

  private[lobby] lazy val tube = Tube[Message](
    reader = Json.reads[Message],
    writer = Json.writes[Message],
    flags = Seq(_.NoId))
}
