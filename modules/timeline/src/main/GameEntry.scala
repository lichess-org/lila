package lila.timeline

case class GameEntry(
    gameId: String,
    whiteName: String,
    blackName: String,
    whiteId: Option[String],
    blackId: Option[String],
    variant: String,
    rated: Boolean,
    clock: Option[String]) {

  def players: List[(String, Option[String])] = List(
    whiteName -> whiteId,
    blackName -> blackId)
}

object GameEntry {

  import lila.db.JsTube
  import play.api.libs.json._

  private[timeline] lazy val tube = JsTube(
    Json.reads[GameEntry], 
    Json.writes[GameEntry],
    Seq(_.NoId)) 
}
