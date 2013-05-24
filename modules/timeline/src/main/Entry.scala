package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._

case class Entry(
    user: String,
    typ: String,
    data: JsObject,
    date: DateTime) {

  def similarTo(other: Entry) =
    (user == other.user) &&
      (typ == other.typ) &&
      (data == other.data)
}

object Entry {

  def make(user: String, typ: String, data: JsValue): Option[Entry] =
    data.asOpt[JsObject] map { Entry(user, typ, _, DateTime.now) }

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[timeline] lazy val tube = Tube(
    (__.json update (readDate('date))) andThen Json.reads[Entry],
    Json.writes[Entry] andThen (__.json update writeDate('date)),
    Seq(_.NoId))
}
