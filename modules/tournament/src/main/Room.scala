package lila.tournament

case class Room(id: String, messages: List[String]) {

  def nonEmpty = messages.nonEmpty
}

object Room {

  import lila.db.Tube
  import play.api.libs.json._

  private[tournament] lazy val tube = Tube(
    Json.reads[Room], 
    Json.writes[Room])

  case class Message(userId: Option[String], text: String) 

  def encode(msg: Message): String = (msg.userId | "_") + " " + msg.text

  def decode(encoded: String): Message = encoded.takeWhile(' ' !=) match {
    case "_"  ⇒ Message(none, encoded.drop(2))
    case user ⇒ Message(user.some, encoded.drop(user.size + 1))
  }
}
