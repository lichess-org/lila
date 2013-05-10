package lila.round

case class WatcherRoom(id: String, messages: List[String]) {

  def rematchCopy(id: String, nb: Int) = copy(
    id = id,
    messages = messages takeRight nb)

  def nonEmpty = messages.nonEmpty
}

object WatcherRoom {

  import lila.db.Tube
  import play.api.libs.json._

  private[round] lazy val tube = Tube(
    Json.reads[WatcherRoom], 
    Json.writes[WatcherRoom])

  def encode(userId: Option[String], text: String): String =
    ~userId + "|" + text

  def decode(encoded: String): (Option[String], String) =
    encoded.span('|' !=) match {
      case (userId, rest) ⇒ Some(userId).filter(_.nonEmpty) -> rest 
    }
}
