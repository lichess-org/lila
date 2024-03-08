package lila.study

import chess.Color
import chess.format.pgn.Tags
import chess.format.{ Fen, Uci }
import chess.{ ByColor, Centis, Color, Outcome, PlayerName, PlayerTitle, Elo }
import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import com.github.blemale.scaffeine.AsyncLoadingCache

case class ChapterPreview(
    id: StudyChapterId,
    name: StudyChapterName,
    players: Option[ChapterPreview.Players],
    orientation: Color,
    fen: Fen.Epd,
    lastMove: Option[Uci],
    lastMoveAt: Option[Instant],
    result: Option[Option[Outcome]]
) extends Chapter.Metadata:
  def playing = lastMove.isDefined && result.contains(None)

final class ChapterPreviewApi( chapterRepo: ChapterRepo, cacheApi: lila.memo.CacheApi)(using Executor):

  import ChapterPreview.bson.{projection, given}
  import ChapterPreview.json.given

  private val listCache: AsyncLoadingCache[StudyId, JsValue] =
    cacheApi.scaffeine
      .expireAfterWrite(3 seconds)
      .buildAsyncFuture[StudyId, JsValue]: studyId =>
        chapterRepo.coll:
          _.find(chapterRepo.$studyId(studyId))
            .sort(chapterRepo.$sortOrder)
            .cursor[ChapterPreview]()
            .listAll()
            .map(Json.toJson)

  def list(studyId: StudyId): Fu[JsValue] = listCache.get(studyId)

  def invalidate(studyId: StudyId): Unit =
    listCache.synchronous().invalidate(studyId)

object ChapterPreview:

  case class Player(name: PlayerName, title: Option[PlayerTitle], rating: Option[Elo], clock: Option[Centis])

  type Players = ByColor[Player]

  def players(clocks: ByColor[Option[Centis]])(tags: Tags): Option[Players] =
    val names = tags.names
    names.exists(_.isDefined) option:
      names zip tags.titles zip tags.elos zip clocks map:
        case (((n, t), e), c) => Player(n | PlayerName("Unknown player"), t, e, c)

  object json:
    import lila.common.Json.{ writeAs, given }

    given Writes[ChapterPreview.Player] = Writes[ChapterPreview.Player]: p =>
      Json
        .obj("name" -> p.name)
        .add("title" -> p.title)
        .add("rating" -> p.rating)
        .add("clock" -> p.clock)

    given Writes[ChapterPreview.Players] = Writes[ChapterPreview.Players]: players =>
      Json.obj("white" -> players.white, "black" -> players.black)

    given Writes[Outcome] = writeAs(_.toString.replace("1/2", "½"))

    given chapterPreviewWrites: OWrites[ChapterPreview] = c =>
      Json.obj(
        "id"          -> c.id,
        "name"        -> c.name,
        "players"     -> c.players,
        "orientation" -> c.orientation,
        "fen"         -> c.fen,
        "lastMove"    -> c.lastMove,
        "lastMoveAt"  -> c.lastMoveAt,
        "status"      -> c.statusStr
      )

  object bson:
    import BSONHandlers.given

    val projection = $doc(
      "name"        -> true,
      "denorm"      -> true,
      "tags"        -> true,
      "lastMoveAt"  -> "$relay.lastMoveAt",
      "orientation" -> "$setup.orientation"
    )

    given BSONDocumentReader[ChapterPreview] = BSONDocumentReader.option[ChapterPreview]: doc =>
      for
        id   <- doc.getAsOpt[StudyChapterId]("_id")
        name <- doc.getAsOpt[StudyChapterName]("name")
        lastMoveAt  = doc.getAsOpt[Instant]("lastMoveAt")
        denorm      = doc.child("denorm")
        fen         = denorm.flatMap(_.getAsOpt[Fen.Epd]("fen"))
        lastMove    = denorm.flatMap(_.getAsOpt[Uci]("uci"))
        tags        = doc.getAsOpt[Tags]("tags")
        orientation = doc.getAsOpt[Color]("orientation") | Color.White
        clocks      = ByColor[Option[Centis]](_ => none)
      yield ChapterPreview(
        id = id,
        name = name,
        players = tags.flatMap(ChapterPreview.players(clocks)),
        orientation = orientation,
        fen = fen | Fen.initial,
        lastMove = lastMove,
        lastMoveAt = lastMoveAt,
        result = tags.flatMap(_(_.Result)).map(Outcome.fromResult)
      )
