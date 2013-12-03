package lila.i18n

import org.joda.time.DateTime

private[i18n] case class Translation(
    id: Int,
    code: String, // 2-chars code
    text: String,
    author: Option[String] = None,
    comment: Option[String] = None,
    createdAt: DateTime) {

  def lines = text.split("\n").toList

  override def toString = "#%d %s".format(id, code)
}

private[i18n] object Translation {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "author" -> none[String],
    "comment" -> none[String])

  private[i18n] val tube = JsTube(
    (__.json update (
      merge(defaults) andThen readDate('createdAt)
    )) andThen Json.reads[Translation],
    Json.writes[Translation] andThen (__.json update writeDate('createdAt))
  ) 
}
