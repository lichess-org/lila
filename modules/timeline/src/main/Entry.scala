package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._
import scala.util.{ Try, Success, Failure }

import lila.db.dsl._
import lila.hub.actorApi.timeline._

case class Entry(
    _id: BSONObjectID,
    users: List[String],
    typ: String,
    chan: Option[String],
    data: Bdoc,
    date: DateTime) {

  import Entry._
  import atomBsonHandlers._

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  lazy val decode: Option[Atom] = Try(typ match {
    case "follow"       => followHandler.read(data)
    case "team-join"    => teamJoinHandler.read(data)
    case "team-create"  => teamCreateHandler.read(data)
    case "forum-post"   => forumPostHandler.read(data)
    case "note-create"  => noteCreateHandler.read(data)
    case "tour-join"    => tourJoinHandler.read(data)
    case "qa-question"  => qaQuestionHandler.read(data)
    case "qa-answer"    => qaAnswerHandler.read(data)
    case "qa-comment"   => qaCommentHandler.read(data)
    case "game-end"     => gameEndHandler.read(data)
    case "simul-create" => simulCreateHandler.read(data)
    case "simul-join"   => simulJoinHandler.read(data)
    case "study-create" => studyCreateHandler.read(data)
    case _              => sys error s"Unhandled atom type: $typ"
  }) match {
    case Success(atom) => Some(atom)
    case Failure(err) =>
      lila.log("timeline").warn(err.getMessage)
      none
  }

  def okForKid = decode ?? (_.okForKid)
}

object Entry {

  private def toBson[A](data: A)(implicit writer: BSONDocumentWriter[A]) = writer write data
  private def fromBson[A](bson: Bdoc)(implicit reader: BSONDocumentReader[A]) = reader read bson

  private[timeline] def make(users: List[String], data: Atom): Entry = {
    import atomBsonHandlers._
    data match {
      case d: Follow      => "follow" -> toBson(d)
      case d: TeamJoin    => "team-join" -> toBson(d)
      case d: TeamCreate  => "team-create" -> toBson(d)
      case d: ForumPost   => "forum-post" -> toBson(d)
      case d: NoteCreate  => "note-create" -> toBson(d)
      case d: TourJoin    => "tour-join" -> toBson(d)
      case d: QaQuestion  => "qa-question" -> toBson(d)
      case d: QaAnswer    => "qa-answer" -> toBson(d)
      case d: QaComment   => "qa-comment" -> toBson(d)
      case d: GameEnd     => "game-end" -> toBson(d)
      case d: SimulCreate => "simul-create" -> toBson(d)
      case d: SimulJoin   => "simul-join" -> toBson(d)
      case d: StudyCreate => "study-create" -> toBson(d)(studyCreateHandler)
    }
  } match {
    case (typ, bson) =>
      new Entry(BSONObjectID.generate, users, typ, data.channel.some, bson, DateTime.now)
  }

  object atomBsonHandlers {
    implicit val followHandler = Macros.handler[Follow]
    implicit val teamJoinHandler = Macros.handler[TeamJoin]
    implicit val teamCreateHandler = Macros.handler[TeamCreate]
    implicit val forumPostHandler = Macros.handler[ForumPost]
    implicit val noteCreateHandler = Macros.handler[NoteCreate]
    implicit val tourJoinHandler = Macros.handler[TourJoin]
    implicit val qaQuestionHandler = Macros.handler[QaQuestion]
    implicit val qaAnswerHandler = Macros.handler[QaAnswer]
    implicit val qaCommentHandler = Macros.handler[QaComment]
    implicit val gameEndHandler = Macros.handler[GameEnd]
    implicit val simulCreateHandler = Macros.handler[SimulCreate]
    implicit val simulJoinHandler = Macros.handler[SimulJoin]
    implicit val studyCreateHandler = Macros.handler[StudyCreate]
  }

  object atomJsonWrite {
    implicit val followWrite = Json.writes[Follow]
    implicit val teamJoinWrite = Json.writes[TeamJoin]
    implicit val teamCreateWrite = Json.writes[TeamCreate]
    implicit val forumPostWrite = Json.writes[ForumPost]
    implicit val noteCreateWrite = Json.writes[NoteCreate]
    implicit val tourJoinWrite = Json.writes[TourJoin]
    implicit val qaQuestionWrite = Json.writes[QaQuestion]
    implicit val qaAnswerWrite = Json.writes[QaAnswer]
    implicit val qaCommentWrite = Json.writes[QaComment]
    implicit val gameEndWrite = Json.writes[GameEnd]
    implicit val simulCreateWrite = Json.writes[SimulCreate]
    implicit val simulJoinWrite = Json.writes[SimulJoin]
    implicit val studyCreateWrite = Json.writes[StudyCreate]
    implicit val atomWrite = Writes[Atom] {
      case d: Follow      => followWrite writes d
      case d: TeamJoin    => teamJoinWrite writes d
      case d: TeamCreate  => teamCreateWrite writes d
      case d: ForumPost   => forumPostWrite writes d
      case d: NoteCreate  => noteCreateWrite writes d
      case d: TourJoin    => tourJoinWrite writes d
      case d: QaQuestion  => qaQuestionWrite writes d
      case d: QaAnswer    => qaAnswerWrite writes d
      case d: QaComment   => qaCommentWrite writes d
      case d: GameEnd     => gameEndWrite writes d
      case d: SimulCreate => simulCreateWrite writes d
      case d: SimulJoin   => simulJoinWrite writes d
      case d: StudyCreate => studyCreateWrite writes d
    }
  }

  implicit val EntryBSONHandler = Macros.handler[Entry]

  implicit val entryWrites = OWrites[Entry] { e =>
    import atomJsonWrite._
    Json.obj(
      "type" -> e.typ,
      "data" -> e.decode,
      "date" -> e.date)
  }
}
