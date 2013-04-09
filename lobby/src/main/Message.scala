package lila.lobby

import play.api.libs.json._
import reactivemongo.bson._

private[lobby] case class Message(
    id: BSONObjectID,
    username: String,
    text: String) {

  def render: JsObject = Json.obj("txt" -> text, "u" -> username)

  def isEmpty = text.isEmpty
}

object Message {

  def make(username: String, text: String) =
    new Message(BSONObjectID.generate, username, text)

  import lila.db.Tube
  import Tube.Helpers._

  private[lobby] lazy val tube = Tube[Message](
    reader = (__.json update readOid) andThen Json.reads[Message],
    writer = Json.writes[Message],
    writeTransformer = (__.json update writeOid).some)
}
