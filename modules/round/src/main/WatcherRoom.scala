package lila.round

case class WatcherRoom(id: String, messages: List[String]) {

  def decodedMessages = messages map WatcherRoom.decode

  def rematchCopy(id: String, nb: Int) = copy(
    id = id,
    messages = messages takeRight nb)

  def nonEmpty = messages.nonEmpty
}

object WatcherRoom {

  import lila.db.JsTube
  import play.api.libs.json._

  private[round] lazy val tube = JsTube(
    Json.reads[WatcherRoom], 
    Json.writes[WatcherRoom])

  def encode(username: Option[String], text: String): String =
    ~username + "|" + text

  def decode(encoded: String): (Option[String], String) =
    encoded.span('|' !=) match {
      case (username, rest) ⇒ Some(username).filter(_.nonEmpty) -> rest.drop(1)
    }
}
