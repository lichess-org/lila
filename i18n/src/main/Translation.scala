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
}

object Translations {

  import lila.db.JsonTube
  import JsonTube.Helpers._
  import play.api.libs.json._

  val defaults = Json.obj(
    "author" -> none[String],
    "comment" -> none[String])

  val json = JsonTube(
    reads = (__.json update (
      merge(defaults) andThen readDate('createdAt)
    )) andThen Json.reads[Translation],
    writes = Json.writes[Translation],
    writeTransformer = (__.json update (
      writeDate('createdAt)
    )).some
  )
}
