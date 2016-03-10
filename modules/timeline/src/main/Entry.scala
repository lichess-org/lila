package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import lila.hub.actorApi.timeline._
import lila.hub.actorApi.timeline.atomFormat._

case class Entry(
    _id: BSONObjectID,
    users: List[String],
    typ: String,
    chan: Option[String],
    data: JsObject,
    date: DateTime) {

  import Entry._

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  lazy val decode: Option[Atom] = (typ match {
    case "follow"       => Json.fromJson[Follow](data)
    case "team-join"    => Json.fromJson[TeamJoin](data)
    case "team-create"  => Json.fromJson[TeamCreate](data)
    case "forum-post"   => Json.fromJson[ForumPost](data)
    case "note-create"  => Json.fromJson[NoteCreate](data)
    case "tour-join"    => Json.fromJson[TourJoin](data)
    case "qa-question"  => Json.fromJson[QaQuestion](data)
    case "qa-answer"    => Json.fromJson[QaAnswer](data)
    case "qa-comment"   => Json.fromJson[QaComment](data)
    case "game-end"     => Json.fromJson[GameEnd](data)
    case "simul-create" => Json.fromJson[SimulCreate](data)
    case "simul-join"   => Json.fromJson[SimulJoin](data)
  }).asOpt

  def okForKid = decode ?? (_.okForKid)
}

object Entry {

  private[timeline] def make(users: List[String], data: Atom): Option[Entry] = (data match {
    case d: Follow      => "follow" -> Json.toJson(d)
    case d: TeamJoin    => "team-join" -> Json.toJson(d)
    case d: TeamCreate  => "team-create" -> Json.toJson(d)
    case d: ForumPost   => "forum-post" -> Json.toJson(d)
    case d: NoteCreate  => "note-create" -> Json.toJson(d)
    case d: TourJoin    => "tour-join" -> Json.toJson(d)
    case d: QaQuestion  => "qa-question" -> Json.toJson(d)
    case d: QaAnswer    => "qa-answer" -> Json.toJson(d)
    case d: QaComment   => "qa-comment" -> Json.toJson(d)
    case d: GameEnd     => "game-end" -> Json.toJson(d)
    case d: SimulCreate => "simul-create" -> Json.toJson(d)
    case d: SimulJoin   => "simul-join" -> Json.toJson(d)
  }) match {
    case (typ, json) => json.asOpt[JsObject] map {
      new Entry(BSONObjectID.generate, users, typ, data.channel.some, _, DateTime.now)
    }
  }

  import play.modules.reactivemongo.json.ImplicitBSONHandlers._
  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  implicit val EntryBSONHandler = Macros.handler[Entry]

  implicit val entryWrites = OWrites[Entry] { e =>
    Json.obj(
      "type" -> e.typ,
      "data" -> e.data,
      "date" -> e.date)
  }
}
