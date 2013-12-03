package lila.round

case class Room(id: String, messages: List[String]) {

  def decodedMessages = messages map Room.decode

  def rematchCopy(id: String, nb: Int) = copy(
    id = id,
    messages = messages takeRight nb)

  def nonEmpty = messages.nonEmpty
}

object Room {

  import lila.db.JsTube
  import play.api.libs.json._

  private[round] lazy val tube = JsTube(Json.reads[Room], Json.writes[Room]) 

  def encode(author: String, text: String): String = (author match {
    case "white" ⇒ "w"
    case "black" ⇒ "b"
    case _       ⇒ "s"
  }) + text

  def decode(encoded: String): (String, String) = (encoded take 1 match {
    case "w" ⇒ "white"
    case "b" ⇒ "black"
    case _   ⇒ "system"
  }, encoded drop 1)
}
