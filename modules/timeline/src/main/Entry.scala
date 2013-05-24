package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._

case class Entry(
  user: String,
  typ: String,
  data: JsObject,
  date: DateTime)

object Entry {

  def make(user: String, typ: String, data: JsValue): Option[Entry] =
    data.asOpt[JsObject] map { Entry(user, typ, _, DateTime.now) }

  import lila.db.Tube
  import play.api.libs.json._

  private[timeline] lazy val tube = Tube(
    Json.reads[Entry],
    Json.writes[Entry],
    Seq(_.NoId))
}
