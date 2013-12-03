package lila.tournament

case class Room(id: String, messages: List[String]) {

  def nonEmpty = messages.nonEmpty

  def decodedMessages = messages map Room.decode
}

object Room {

  import lila.db.JsTube
  import play.api.libs.json._

  private[tournament] lazy val tube = JsTube(
    Json.reads[Room], 
    Json.writes[Room])

  case class Message(userId: Option[String], text: String) 

  def encode(msg: Message): String = (msg.userId | "_") + " " + msg.text

  def decode(encoded: String): Message = encoded.takeWhile(' ' !=) match {
    case "_"  ⇒ Message(none, encoded.drop(2))
    case user ⇒ Message(user.some, encoded.drop(user.size + 1))
  }
}
