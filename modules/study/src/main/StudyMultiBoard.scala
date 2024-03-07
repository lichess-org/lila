package lila.study

import BSONHandlers._
import shogi.Color
import shogi.format.Tags
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.{ Standard, Variant }
import com.github.blemale.scaffeine.AsyncLoadingCache
import JsonView._
import play.api.libs.json._
import reactivemongo.api.bson._
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

  private val playingSelector = $doc("tags" -> "Result:*")

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

    // If broadcasts are ever implemented, this needs some changes
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
                      "args" -> $arr("$root"),
                      "body" -> """function(root) {
                    |return root['ÿ'] !== undefined ? {sfen:root['ÿ'].f} : {sfen:root['þ'].is}; }""".stripMargin
                    )
                  ),
                  "tags"        -> "$tags",
                  "orientation" -> "$setup.orientation",
                  "variant"     -> "$setup.variant",
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
            tags    = doc.getAsOpt[Tags]("tags")
            variant = doc.getAsOpt[Int]("variant").flatMap(v => Variant(v)) | Standard
            comp <- doc.getAsOpt[Bdoc]("comp")
            lastUsi = comp.getAsOpt[Usi]("usi")
            sfen    = comp.getAsOpt[Sfen]("sfen").getOrElse(variant.initialSfen)
          } yield ChapterPreview(
            id = id,
            name = name,
            players = tags flatMap ChapterPreview.players,
            variant = variant,
            orientation = doc.getAsOpt[Color]("orientation") | Color.Sente,
            sfen = sfen,
            lastUsi = lastUsi,
            playing = lastUsi.isDefined && tags.flatMap(_(_.Result)).has("*")
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
        Json.obj("sente" -> players.sente, "gote" -> players.gote)
    }

    implicit val previewWriter: Writes[ChapterPreview] = Json.writes[ChapterPreview]
  }
}

object StudyMultiBoard {

  case class ChapterPreview(
      id: Chapter.Id,
      name: Chapter.Name,
      players: Option[ChapterPreview.Players],
      variant: Variant,
      orientation: Color,
      sfen: Sfen,
      lastUsi: Option[Usi],
      playing: Boolean
  )

  object ChapterPreview {

    case class Player(name: String, title: Option[String], rating: Option[Int])

    type Players = Color.Map[Player]

    def players(tags: Tags): Option[Players] =
      for {
        sName <- tags(_.Sente)
        gName <- tags(_.Gote)
      } yield Color.Map(
        sente = Player(sName, tags(_.SenteTitle), tags(_.SenteElo) flatMap (_.toIntOption)),
        gote = Player(gName, tags(_.GoteTitle), tags(_.GoteElo) flatMap (_.toIntOption))
      )
  }
}
