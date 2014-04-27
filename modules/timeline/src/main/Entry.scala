package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.hub.actorApi.timeline._
import lila.hub.actorApi.timeline.atomFormat._

case class Entry(
    users: List[String],
    typ: String,
    data: JsObject,
    date: DateTime) {

  import Entry._

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  def decode: Option[Atom] = (typ match {
    case "follow"      => Json.fromJson[Follow](data)
    case "team-join"   => Json.fromJson[TeamJoin](data)
    case "team-create" => Json.fromJson[TeamCreate](data)
    case "forum-post"  => Json.fromJson[ForumPost](data)
    case "note-create"  => Json.fromJson[NoteCreate](data)
  }).asOpt
}

object Entry {

  private[timeline] def make(users: List[String], data: Atom): Option[Entry] = (data match {
    case d: Follow     => "follow" -> Json.toJson(d)
    case d: TeamJoin   => "team-join" -> Json.toJson(d)
    case d: TeamCreate => "team-create" -> Json.toJson(d)
    case d: ForumPost  => "forum-post" -> Json.toJson(d)
    case d: NoteCreate  => "note-create" -> Json.toJson(d)
  }) match {
    case (typ, json) => json.asOpt[JsObject] map { new Entry(users, typ, _, DateTime.now) }
  }

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[timeline] lazy val tube = JsTube(
    (__.json update (readDate('date))) andThen Json.reads[Entry],
    Json.writes[Entry] andThen (__.json update writeDate('date)),
    Seq(_.NoId))
}
