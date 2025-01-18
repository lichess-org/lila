package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.api.bson._
import scala.util.{ Failure, Success, Try }

import lila.common.Json.jodaWrites
import lila.db.dsl._
import lila.hub.actorApi.timeline._

case class Entry(
    _id: BSONObjectID,
    typ: String,
    chan: Option[String],
    data: Bdoc,
    date: DateTime
) {

  import Entry._
  import atomBsonHandlers._

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  case object Deprecated extends lila.base.LilaException {
    val message = "Deprecated timeline entry"
  }

  lazy val decode: Option[Atom] = Try(typ match {
    case "follow"              => followHandler.readTry(data).get
    case "team-join"           => teamJoinHandler.readTry(data).get
    case "team-create"         => teamCreateHandler.readTry(data).get
    case "forum-post"          => forumPostHandler.readTry(data).get
    case "tour-join"           => tourJoinHandler.readTry(data).get
    case "game-end"            => gameEndHandler.readTry(data).get
    case "simul-create"        => simulCreateHandler.readTry(data).get
    case "simul-join"          => simulJoinHandler.readTry(data).get
    case "study-create"        => studyCreateHandler.readTry(data).get
    case "study-like"          => studyLikeHandler.readTry(data).get
    case "plan-start"          => planStartHandler.readTry(data).get
    case "blog-post"           => blogPostHandler.readTry(data).get
    case "stream-start"        => streamStartHandler.readTry(data).get
    case "system-notification" => systemNotificationHandler.readTry(data).get
    case "note-create"         => throw Deprecated
    case _                     => sys error s"Unhandled atom type: $typ"
  }) match {
    case Success(atom)       => Some(atom)
    case Failure(Deprecated) => none
    case Failure(err) =>
      lila.log("timeline").warn(err.getMessage)
      none
  }

  def userIds = decode.??(_.userIds)

  def okForKid = decode ?? (_.okForKid)
}

object Entry {

  case class ForUsers(entry: Entry, userIds: List[String])

  private def toBson[A](data: A)(implicit writer: BSONDocumentWriter[A]) = writer.writeTry(data).get

  private[timeline] def make(data: Atom): Entry = {
    import atomBsonHandlers._
    data match {
      case d: Follow             => "follow"              -> toBson(d)
      case d: TeamJoin           => "team-join"           -> toBson(d)
      case d: TeamCreate         => "team-create"         -> toBson(d)
      case d: ForumPost          => "forum-post"          -> toBson(d)
      case d: TourJoin           => "tour-join"           -> toBson(d)
      case d: GameEnd            => "game-end"            -> toBson(d)
      case d: SimulCreate        => "simul-create"        -> toBson(d)
      case d: SimulJoin          => "simul-join"          -> toBson(d)
      case d: StudyCreate        => "study-create"        -> toBson(d)(studyCreateHandler)
      case d: StudyLike          => "study-like"          -> toBson(d)(studyLikeHandler)
      case d: PlanStart          => "plan-start"          -> toBson(d)(planStartHandler)
      case d: BlogPost           => "blog-post"           -> toBson(d)(blogPostHandler)
      case d: StreamStart        => "stream-start"        -> toBson(d)(streamStartHandler)
      case d: SystemNotification => "system-notification" -> toBson(d)
    }
  } match {
    case (typ, bson) =>
      new Entry(BSONObjectID.generate(), typ, data.channel.some, bson, DateTime.now)
  }

  object atomBsonHandlers {
    implicit val followHandler: BSONDocumentHandler[Follow]             = Macros.handler[Follow]
    implicit val teamJoinHandler: BSONDocumentHandler[TeamJoin]           = Macros.handler[TeamJoin]
    implicit val teamCreateHandler: BSONDocumentHandler[TeamCreate]         = Macros.handler[TeamCreate]
    implicit val forumPostHandler: BSONDocumentHandler[ForumPost]          = Macros.handler[ForumPost]
    implicit val tourJoinHandler: BSONDocumentHandler[TourJoin]           = Macros.handler[TourJoin]
    implicit val gameEndHandler: BSONDocumentHandler[GameEnd]            = Macros.handler[GameEnd]
    implicit val simulCreateHandler: BSONDocumentHandler[SimulCreate]        = Macros.handler[SimulCreate]
    implicit val simulJoinHandler: BSONDocumentHandler[SimulJoin]          = Macros.handler[SimulJoin]
    implicit val studyCreateHandler: BSONDocumentHandler[StudyCreate]        = Macros.handler[StudyCreate]
    implicit val studyLikeHandler: BSONDocumentHandler[StudyLike]          = Macros.handler[StudyLike]
    implicit val planStartHandler: BSONDocumentHandler[PlanStart]          = Macros.handler[PlanStart]
    implicit val blogPostHandler: BSONDocumentHandler[BlogPost]           = Macros.handler[BlogPost]
    implicit val streamStartHandler: BSONDocumentHandler[StreamStart]        = Macros.handler[StreamStart]
    implicit val systemNotificationHandler: BSONDocumentHandler[SystemNotification] = Macros.handler[SystemNotification]
  }

  object atomJsonWrite {
    implicit val followWrite: OWrites[Follow]             = Json.writes[Follow]
    implicit val teamJoinWrite: OWrites[TeamJoin]           = Json.writes[TeamJoin]
    implicit val teamCreateWrite: OWrites[TeamCreate]         = Json.writes[TeamCreate]
    implicit val forumPostWrite: OWrites[ForumPost]          = Json.writes[ForumPost]
    implicit val tourJoinWrite: OWrites[TourJoin]           = Json.writes[TourJoin]
    implicit val gameEndWrite: OWrites[GameEnd]            = Json.writes[GameEnd]
    implicit val simulCreateWrite: OWrites[SimulCreate]        = Json.writes[SimulCreate]
    implicit val simulJoinWrite: OWrites[SimulJoin]          = Json.writes[SimulJoin]
    implicit val studyCreateWrite: OWrites[StudyCreate]        = Json.writes[StudyCreate]
    implicit val studyLikeWrite: OWrites[StudyLike]          = Json.writes[StudyLike]
    implicit val planStartWrite: OWrites[PlanStart]          = Json.writes[PlanStart]
    implicit val blogPostWrite: OWrites[BlogPost]           = Json.writes[BlogPost]
    implicit val streamStartWrite: OWrites[StreamStart]        = Json.writes[StreamStart]
    implicit val systemNotificationWrite: OWrites[SystemNotification] = Json.writes[SystemNotification]
    implicit val atomWrite: Writes[Atom] = Writes[Atom] {
      case d: Follow             => followWrite writes d
      case d: TeamJoin           => teamJoinWrite writes d
      case d: TeamCreate         => teamCreateWrite writes d
      case d: ForumPost          => forumPostWrite writes d
      case d: TourJoin           => tourJoinWrite writes d
      case d: GameEnd            => gameEndWrite writes d
      case d: SimulCreate        => simulCreateWrite writes d
      case d: SimulJoin          => simulJoinWrite writes d
      case d: StudyCreate        => studyCreateWrite writes d
      case d: StudyLike          => studyLikeWrite writes d
      case d: PlanStart          => planStartWrite writes d
      case d: BlogPost           => blogPostWrite writes d
      case d: StreamStart        => streamStartWrite writes d
      case d: SystemNotification => systemNotificationWrite writes d
    }
  }

  implicit val EntryBSONHandler: BSONDocumentHandler[Entry] = Macros.handler[Entry]

  implicit val entryWrites: OWrites[Entry] = OWrites[Entry] { e =>
    import atomJsonWrite._
    Json.obj(
      "type" -> e.typ,
      "data" -> e.decode,
      "date" -> e.date
    )
  }
}
