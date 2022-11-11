package lila.timeline

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.api.bson._
import scala.util.{ Failure, Success, Try }

import lila.common.Json.jodaWrites
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline._

case class Entry(
    _id: BSONObjectID,
    typ: String,
    chan: Option[String],
    data: Bdoc,
    date: DateTime
) {

  import Entry._

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  lazy val decode: Option[Atom] = atomBsonHandlers.handlers.get(typ).flatMap(_ readOpt data)

  def userIds = decode.??(_.userIds)

  def okForKid = decode.??(_.okForKid)
}

object Entry {

  case class ForUsers(entry: Entry, userIds: List[String])

  private def toBson[A](data: A)(implicit writer: BSONDocumentWriter[A]) = writer.writeTry(data).get

  private[timeline] def make(data: Atom): Entry = {
    import atomBsonHandlers._
    data match {
      case d: Follow        => "follow"          -> toBson(d)
      case d: TeamJoin      => "team-join"       -> toBson(d)
      case d: TeamCreate    => "team-create"     -> toBson(d)
      case d: ForumPost     => "forum-post"      -> toBson(d)
      case d: UblogPost     => "ublog-post"      -> toBson(d)
      case d: TourJoin      => "tour-join"       -> toBson(d)
      case d: GameEnd       => "game-end"        -> toBson(d)
      case d: SimulCreate   => "simul-create"    -> toBson(d)
      case d: SimulJoin     => "simul-join"      -> toBson(d)
      case d: StudyLike     => "study-like"      -> toBson(d)
      case d: PlanStart     => "plan-start"      -> toBson(d)
      case d: PlanRenew     => "plan-renew"      -> toBson(d)
      case d: BlogPost      => "blog-post"       -> toBson(d)
      case d: UblogPostLike => "ublog-post-like" -> toBson(d)
      case d: StreamStart   => "stream-start"    -> toBson(d)
    }
  } match {
    case (typ, bson) =>
      new Entry(BSONObjectID.generate(), typ, data.channel.some, bson, DateTime.now)
  }

  object atomBsonHandlers {
    given BSONDocumentHandler[Follow] = Macros.handler
    given BSONDocumentHandler[TeamJoin] = Macros.handler
    given BSONDocumentHandler[TeamCreate] = Macros.handler
    given BSONDocumentHandler[ForumPost] = Macros.handler
    given BSONDocumentHandler[UblogPost] = Macros.handler
    given BSONDocumentHandler[TourJoin] = Macros.handler
    given BSONDocumentHandler[GameEnd] = Macros.handler
    given BSONDocumentHandler[SimulCreate] = Macros.handler
    given BSONDocumentHandler[SimulJoin] = Macros.handler
    given BSONDocumentHandler[StudyLike] = Macros.handler
    given BSONDocumentHandler[PlanStart] = Macros.handler
    given BSONDocumentHandler[PlanRenew] = Macros.handler
    given BSONDocumentHandler[BlogPost] = Macros.handler
    given BSONDocumentHandler[UblogPostLike] = Macros.handler
    given BSONDocumentHandler[StreamStart] = Macros.handler

    val handlers = Map(
      "follow"          -> followHandler,
      "team-join"       -> teamJoinHandler,
      "team-create"     -> teamCreateHandler,
      "forum-post"      -> forumPostHandler,
      "ublog-post"      -> ublogPostHandler,
      "tour-join"       -> tourJoinHandler,
      "game-end"        -> gameEndHandler,
      "simul-create"    -> simulCreateHandler,
      "simul-join"      -> simulJoinHandler,
      "study-like"      -> studyLikeHandler,
      "plan-start"      -> planStartHandler,
      "plan-renew"      -> planRenewHandler,
      "blog-post"       -> blogPostHandler,
      "ublog-post-like" -> ublogPostLikeHandler,
      "stream-start"    -> streamStartHandler
    )
  }

  object atomJsonWrite {
    val followWrite        = Json.writes[Follow]
    val teamJoinWrite      = Json.writes[TeamJoin]
    val teamCreateWrite    = Json.writes[TeamCreate]
    val forumPostWrite     = Json.writes[ForumPost]
    val ublogPostWrite     = Json.writes[UblogPost]
    val tourJoinWrite      = Json.writes[TourJoin]
    val gameEndWrite       = Json.writes[GameEnd]
    val simulCreateWrite   = Json.writes[SimulCreate]
    val simulJoinWrite     = Json.writes[SimulJoin]
    val studyLikeWrite     = Json.writes[StudyLike]
    val planStartWrite     = Json.writes[PlanStart]
    val planRenewWrite     = Json.writes[PlanRenew]
    val blogPostWrite      = Json.writes[BlogPost]
    val ublogPostLikeWrite = Json.writes[UblogPostLike]
    val streamStartWrite   = Json.writes[StreamStart]
    implicit val atomWrite = Writes[Atom] {
      case d: Follow        => followWrite writes d
      case d: TeamJoin      => teamJoinWrite writes d
      case d: TeamCreate    => teamCreateWrite writes d
      case d: ForumPost     => forumPostWrite writes d
      case d: UblogPost     => ublogPostWrite writes d
      case d: TourJoin      => tourJoinWrite writes d
      case d: GameEnd       => gameEndWrite writes d
      case d: SimulCreate   => simulCreateWrite writes d
      case d: SimulJoin     => simulJoinWrite writes d
      case d: StudyLike     => studyLikeWrite writes d
      case d: PlanStart     => planStartWrite writes d
      case d: PlanRenew     => planRenewWrite writes d
      case d: BlogPost      => blogPostWrite writes d
      case d: UblogPostLike => ublogPostLikeWrite writes d
      case d: StreamStart   => streamStartWrite writes d
    }
  }

  given BSONDocumentHandler[Entry] = Macros.handler

  implicit val entryWrites = OWrites[Entry] { e =>
    import atomJsonWrite._
    Json.obj(
      "type" -> e.typ,
      "data" -> e.decode,
      "date" -> e.date
    )
  }
}
