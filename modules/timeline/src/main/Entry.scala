package lila.timeline

import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.common.Json.given
import lila.core.timeline.*
import lila.db.dsl.{ *, given }

case class Entry(
    _id: BSONObjectID,
    typ: String,
    chan: Option[String],
    data: Bdoc,
    date: Instant
):

  import Entry.*

  def similarTo(other: Entry) = typ == other.typ && data == other.data

  lazy val decode: Option[Atom] = atomBsonHandlers.handlers.get(typ).flatMap(_.readOpt(data))

  def userIds = decode.so(_.userIds)

  def okForKid = decode.so(_.okForKid)

object Entry:

  case class ForUsers(entry: Entry, userIds: List[UserId])

  private def toBson[A](data: A)(using writer: BSONDocumentWriter[A]) = writer.writeTry(data).get

  private[timeline] def make(data: Atom): Entry =
    import atomBsonHandlers.given
    data match
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
      case d: UblogPostLike => "ublog-post-like" -> toBson(d)
      case d: StreamStart   => "stream-start"    -> toBson(d)
    match
      case (typ, bson) =>
        new Entry(BSONObjectID.generate(), typ, data.channel.some, bson, nowInstant)

  object atomBsonHandlers:
    given followHandler: BSONDocumentHandler[Follow]               = Macros.handler
    given teamJoinHandler: BSONDocumentHandler[TeamJoin]           = Macros.handler
    given teamCreateHandler: BSONDocumentHandler[TeamCreate]       = Macros.handler
    given forumPostHandler: BSONDocumentHandler[ForumPost]         = Macros.handler
    given ublogPostHandler: BSONDocumentHandler[UblogPost]         = Macros.handler
    given tourJoinHandler: BSONDocumentHandler[TourJoin]           = Macros.handler
    given gameEndHandler: BSONDocumentHandler[GameEnd]             = Macros.handler
    given simulCreateHandler: BSONDocumentHandler[SimulCreate]     = Macros.handler
    given simulJoinHandler: BSONDocumentHandler[SimulJoin]         = Macros.handler
    given studyLikeHandler: BSONDocumentHandler[StudyLike]         = Macros.handler
    given planStartHandler: BSONDocumentHandler[PlanStart]         = Macros.handler
    given planRenewHandler: BSONDocumentHandler[PlanRenew]         = Macros.handler
    given ublogPostLikeHandler: BSONDocumentHandler[UblogPostLike] = Macros.handler
    given streamStartHandler: BSONDocumentHandler[StreamStart]     = Macros.handler

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
      "ublog-post-like" -> ublogPostLikeHandler,
      "stream-start"    -> streamStartHandler
    )

  object atomJsonWrite:
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
    val ublogPostLikeWrite = Json.writes[UblogPostLike]
    val streamStartWrite   = Json.writes[StreamStart]
    given Writes[Atom] = Writes:
      case d: Follow        => followWrite.writes(d)
      case d: TeamJoin      => teamJoinWrite.writes(d)
      case d: TeamCreate    => teamCreateWrite.writes(d)
      case d: ForumPost     => forumPostWrite.writes(d)
      case d: UblogPost     => ublogPostWrite.writes(d)
      case d: TourJoin      => tourJoinWrite.writes(d)
      case d: GameEnd       => gameEndWrite.writes(d)
      case d: SimulCreate   => simulCreateWrite.writes(d)
      case d: SimulJoin     => simulJoinWrite.writes(d)
      case d: StudyLike     => studyLikeWrite.writes(d)
      case d: PlanStart     => planStartWrite.writes(d)
      case d: PlanRenew     => planRenewWrite.writes(d)
      case d: UblogPostLike => ublogPostLikeWrite.writes(d)
      case d: StreamStart   => streamStartWrite.writes(d)

  given BSONDocumentHandler[Entry] = Macros.handler

  given OWrites[Entry] = OWrites { e =>
    import atomJsonWrite.given
    Json.obj(
      "type" -> e.typ,
      "data" -> e.decode,
      "date" -> e.date
    )
  }
