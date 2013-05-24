package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._

import lila.common.PimpedJson._

case class Entry(
    user: String,
    typ: String,
    data: JsObject,
    date: DateTime) {

  def similarTo(other: Entry) =
    (user == other.user) &&
      (typ == other.typ) &&
      (data == other.data)

  def decode: Option[Entry.Decoded] = typ match {
    case "follow" ⇒ data str "user" map { Entry.Follow(_) }
    case _        ⇒ none
  }
}

object Entry {

  sealed trait Decoded

  case class Follow(userId: String) extends Decoded

  private[timeline] def make(user: String, typ: String, data: JsValue): Option[Entry] =
    data.asOpt[JsObject] map { Entry(user, typ, _, DateTime.now) }

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[timeline] lazy val tube = Tube(
    (__.json update (readDate('date))) andThen Json.reads[Entry],
    Json.writes[Entry] andThen (__.json update writeDate('date)),
    Seq(_.NoId))
}
