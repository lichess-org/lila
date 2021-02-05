package lila.study

import BSONHandlers._
import chess.Color
import chess.format.pgn.Tags
import chess.format.{ FEN, Uci }
import com.github.blemale.scaffeine.AsyncLoadingCache
import JsonView._
import play.api.libs.json._
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.config.MaxPerPage
import lila.common.paginator.AdapterLike
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.db.dsl._

final class StudyMultiBoard(
    chapterRepo: ChapterRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val maxPerPage = MaxPerPage(9)

  import StudyMultiBoard._
  import handlers._

  def json(studyId: Study.Id, page: Int, playing: Boolean): Fu[JsObject] = {
    if (page == 1 && !playing) firstPageCache.get(studyId)
    else fetch(studyId, page, playing)
  } map { PaginatorJson(_) }

  private val firstPageCache: AsyncLoadingCache[Study.Id, Paginator[ChapterPreview]] =
    cacheApi.scaffeine
      .refreshAfterWrite(4 seconds)
      .expireAfterAccess(10 minutes)
      .buildAsyncFuture[Study.Id, Paginator[ChapterPreview]] { fetch(_, 1, playing = false) }

  private val playingSelector = $doc("tags" -> "Result:*", "root.n.0" $exists true)

  private def fetch(studyId: Study.Id, page: Int, playing: Boolean): Fu[Paginator[ChapterPreview]] =
    Paginator[ChapterPreview](
      new ChapterPreviewAdapter(studyId, playing),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final private class ChapterPreviewAdapter(studyId: Study.Id, playing: Boolean)
      extends AdapterLike[ChapterPreview] {

    private val selector = $doc("studyId" -> studyId) ++ playing.??(playingSelector)

    def nbResults: Fu[Int] = chapterRepo.coll(_.countSel(selector))

    def slice(offset: Int, length: Int): Fu[Seq[ChapterPreview]] =
      chapterRepo
        .coll {
          _.aggregateList(length, readPreference = readPref) { framework =>
            import framework._
            Match(selector) -> List(
              Sort(Ascending("order")),
              Skip(offset),
              Limit(length),
              Project(
                $doc(
                  "comp" -> $doc(
                    "$function" -> $doc(
                      "lang" -> "js",
                      "args" -> $arr("$root", "$tags"),
                      "body" -> """function(root, tags) {
                    |tags = tags.filter(t => t.startsWith('White') || t.startsWith('Black') || t.startsWith('Result'));
                    |const node = tags.length ? Object.keys(root).reduce((acc, i) => (root[i].p > acc.p) ? root[i] : acc, root['_']) : root['_'];
                    |return {node:{fen:node.f,uci:node.u},tags} }""".stripMargin
                    )
                  ),
                  "orientation" -> "$setup.orientation",
                  "name"        -> true
                )
              )
            )
          }
        }
        .map { r =>
          for {
            doc  <- r
            id   <- doc.getAsOpt[Chapter.Id]("_id")
            name <- doc.getAsOpt[Chapter.Name]("name")
            comp <- doc.getAsOpt[Bdoc]("comp")
            node <- comp.getAsOpt[Bdoc]("node")
            fen  <- node.getAsOpt[FEN]("fen")
            lastMove = node.getAsOpt[Uci]("uci")
            tags     = comp.getAsOpt[Tags]("tags")
          } yield ChapterPreview(
            id = id,
            name = name,
            players = tags flatMap ChapterPreview.players,
            orientation = doc.getAsOpt[Color]("orientation") | Color.White,
            fen = fen,
            lastMove = lastMove,
            playing = lastMove.isDefined && tags.flatMap(_(_.Result)).has("*")
          )
        }
  }

  private object handlers {

    implicit val previewPlayerWriter: Writes[ChapterPreview.Player] = Writes[ChapterPreview.Player] { p =>
      Json
        .obj("name" -> p.name)
        .add("title" -> p.title)
        .add("rating" -> p.rating)
    }

    implicit val previewPlayersWriter: Writes[ChapterPreview.Players] = Writes[ChapterPreview.Players] {
      players =>
        Json.obj("white" -> players.white, "black" -> players.black)
    }

    implicit val previewWriter: Writes[ChapterPreview] = Json.writes[ChapterPreview]
  }
}

object StudyMultiBoard {

  case class ChapterPreview(
      id: Chapter.Id,
      name: Chapter.Name,
      players: Option[ChapterPreview.Players],
      orientation: Color,
      fen: FEN,
      lastMove: Option[Uci],
      playing: Boolean
  )

  object ChapterPreview {

    case class Player(name: String, title: Option[String], rating: Option[Int])

    type Players = Color.Map[Player]

    def players(tags: Tags): Option[Players] =
      for {
        wName <- tags(_.White)
        bName <- tags(_.Black)
      } yield Color.Map(
        white = Player(wName, tags(_.WhiteTitle), tags(_.WhiteElo) flatMap (_.toIntOption)),
        black = Player(bName, tags(_.BlackTitle), tags(_.BlackElo) flatMap (_.toIntOption))
      )
  }
}
