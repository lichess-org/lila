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
    case "follow"       => followHandler.readTry(data).get
    case "team-join"    => teamJoinHandler.readTry(data).get
    case "team-create"  => teamCreateHandler.readTry(data).get
    case "forum-post"   => forumPostHandler.readTry(data).get
    case "ublog-post"   => ublogPostHandler.readTry(data).get
    case "tour-join"    => tourJoinHandler.readTry(data).get
    case "game-end"     => gameEndHandler.readTry(data).get
    case "simul-create" => simulCreateHandler.readTry(data).get
    case "simul-join"   => simulJoinHandler.readTry(data).get
    case "study-like"   => studyLikeHandler.readTry(data).get
    case "plan-start"   => planStartHandler.readTry(data).get
    case "plan-renew"   => planRenewHandler.readTry(data).get
    case "blog-post"    => blogPostHandler.readTry(data).get
    case "stream-start" => streamStartHandler.readTry(data).get
    case _              => sys error s"Unhandled atom type: $typ"
  }) match {
    case Success(atom)       => Some(atom)
    case Failure(Deprecated) => none
    case Failure(err) =>
      lila.log("timeline").warn(err.getMessage)
      none
  }

  def userIds = decode.??(_.userIds)

  def okForKid = decode.??(_.okForKid)
}

object Entry {

  case class ForUsers(entry: Entry, userIds: List[String])

  private def toBson[A](data: A)(implicit writer: BSONDocumentWriter[A]) = writer.writeTry(data).get

  private[timeline] def make(data: Atom): Entry = {
    import atomBsonHandlers._
    data match {
      case d: Follow      => "follow"       -> toBson(d)
      case d: TeamJoin    => "team-join"    -> toBson(d)
      case d: TeamCreate  => "team-create"  -> toBson(d)
      case d: ForumPost   => "forum-post"   -> toBson(d)
      case d: UblogPost   => "ublog-post"   -> toBson(d)
      case d: TourJoin    => "tour-join"    -> toBson(d)
      case d: GameEnd     => "game-end"     -> toBson(d)
      case d: SimulCreate => "simul-create" -> toBson(d)
      case d: SimulJoin   => "simul-join"   -> toBson(d)
      case d: StudyLike   => "study-like"   -> toBson(d)(studyLikeHandler)
      case d: PlanStart   => "plan-start"   -> toBson(d)(planStartHandler)
      case d: PlanRenew   => "plan-renew"   -> toBson(d)(planRenewHandler)
      case d: BlogPost    => "blog-post"    -> toBson(d)(blogPostHandler)
      case d: StreamStart => "stream-start" -> toBson(d)(streamStartHandler)
    }
  } match {
    case (typ, bson) =>
      new Entry(BSONObjectID.generate(), typ, data.channel.some, bson, DateTime.now)
  }

  object atomBsonHandlers {
    implicit val followHandler      = Macros.handler[Follow]
    implicit val teamJoinHandler    = Macros.handler[TeamJoin]
    implicit val teamCreateHandler  = Macros.handler[TeamCreate]
    implicit val forumPostHandler   = Macros.handler[ForumPost]
    implicit val ublogPostHandler   = Macros.handler[UblogPost]
    implicit val tourJoinHandler    = Macros.handler[TourJoin]
    implicit val gameEndHandler     = Macros.handler[GameEnd]
    implicit val simulCreateHandler = Macros.handler[SimulCreate]
    implicit val simulJoinHandler   = Macros.handler[SimulJoin]
    implicit val studyLikeHandler   = Macros.handler[StudyLike]
    implicit val planStartHandler   = Macros.handler[PlanStart]
    implicit val planRenewHandler   = Macros.handler[PlanRenew]
    implicit val blogPostHandler    = Macros.handler[BlogPost]
    implicit val streamStartHandler = Macros.handler[StreamStart]
  }

  object atomJsonWrite {
    implicit val followWrite      = Json.writes[Follow]
    implicit val teamJoinWrite    = Json.writes[TeamJoin]
    implicit val teamCreateWrite  = Json.writes[TeamCreate]
    implicit val forumPostWrite   = Json.writes[ForumPost]
    implicit val ublogPostWrite   = Json.writes[UblogPost]
    implicit val tourJoinWrite    = Json.writes[TourJoin]
    implicit val gameEndWrite     = Json.writes[GameEnd]
    implicit val simulCreateWrite = Json.writes[SimulCreate]
    implicit val simulJoinWrite   = Json.writes[SimulJoin]
    implicit val studyLikeWrite   = Json.writes[StudyLike]
    implicit val planStartWrite   = Json.writes[PlanStart]
    implicit val planRenewWrite   = Json.writes[PlanRenew]
    implicit val blogPostWrite    = Json.writes[BlogPost]
    implicit val streamStartWrite = Json.writes[StreamStart]
    implicit val atomWrite = Writes[Atom] {
      case d: Follow      => followWrite writes d
      case d: TeamJoin    => teamJoinWrite writes d
      case d: TeamCreate  => teamCreateWrite writes d
      case d: ForumPost   => forumPostWrite writes d
      case d: UblogPost   => ublogPostWrite writes d
      case d: TourJoin    => tourJoinWrite writes d
      case d: GameEnd     => gameEndWrite writes d
      case d: SimulCreate => simulCreateWrite writes d
      case d: SimulJoin   => simulJoinWrite writes d
      case d: StudyLike   => studyLikeWrite writes d
      case d: PlanStart   => planStartWrite writes d
      case d: PlanRenew   => planRenewWrite writes d
      case d: BlogPost    => blogPostWrite writes d
      case d: StreamStart => streamStartWrite writes d
    }
  }

  implicit val EntryBSONHandler = Macros.handler[Entry]

  implicit val entryWrites = OWrites[Entry] { e =>
    import atomJsonWrite._
    Json.obj(
      "type" -> e.typ,
      "data" -> e.decode,
      "date" -> e.date
    )
  }
}
