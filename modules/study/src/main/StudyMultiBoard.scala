package lila.study

import BSONHandlers.given
import chess.{ ByColor, Centis, Color, Outcome }
import chess.format.pgn.Tags
import chess.format.{ Fen, Uci }
import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.common.config.MaxPerPage
import lila.common.paginator.AdapterLike
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.db.dsl.{ *, given }

final class StudyMultiBoard(
    chapterRepo: ChapterRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private val maxPerPage = MaxPerPage(9)

  import StudyMultiBoard.*

  def json(studyId: StudyId, page: Int, playing: Boolean): Fu[JsObject] = {
    if page == 1 && !playing then firstPageCache.get(studyId)
    else fetch(studyId, page, playing)
  } map { PaginatorJson(_) }

  def invalidate(studyId: StudyId): Unit = firstPageCache.synchronous().invalidate(studyId)

  private val firstPageCache: AsyncLoadingCache[StudyId, Paginator[ChapterPreview]] =
    cacheApi.scaffeine
      .refreshAfterWrite(4 seconds)
      .expireAfterAccess(10 minutes)
      .buildAsyncFuture[StudyId, Paginator[ChapterPreview]] { fetch(_, 1, playing = false) }

  private val playingSelector = $doc("tags" -> "Result:*", "relay.path" $ne "")

  private def fetch(studyId: StudyId, page: Int, playing: Boolean): Fu[Paginator[ChapterPreview]] =
    Paginator[ChapterPreview](
      ChapterPreviewAdapter(studyId, playing),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final private class ChapterPreviewAdapter(studyId: StudyId, playing: Boolean)
      extends AdapterLike[ChapterPreview]:

    private val selector = $doc("studyId" -> studyId) ++ playing.so(playingSelector)

    def nbResults: Fu[Int] = chapterRepo.coll(_.countSel(selector))

    def slice(offset: Int, length: Int): Fu[Seq[ChapterPreview]] =
      chapterRepo
        .coll {
          _.aggregateList(length, _.pri): framework =>
            import framework.*
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
                                    tags = tags.filter(t => t.startsWith('White') || t.startsWith('Black') || t.startsWith('Result'));
                                    const [node, clockTicking] = tags.length ?
                                      Object.keys(root).reduce(
                                        ([node, clockTicking, path, pathTicking], i) => {
                                          if (root[i].p > node.p && i.startsWith(path)) {
                                            clockTicking = node;
                                            pathTicking = path;
                                            node = root[i];
                                            path = i;
                                          } else if (clockTicking && root[i].p > clockTicking.p && i.startsWith(pathTicking)) {
                                            clockTicking = root[i];
                                            pathTicking = i;
                                          }
                                          return [node, clockTicking, path, pathTicking]
                                        },
                                        [root['_'], undefined, '', undefined]
                                      ).slice(0, 2) : [root['_'], undefined];
                                    const [whiteClock, blackClock] = clockTicking ? node.f.includes(" b") ? [node.l, clockTicking.l] : [clockTicking.l, node.l] : [undefined, undefined]

                                    return {
                                      node: {
                                        fen: node.f,
                                        uci: node.u,
                                      },
                                      tags,
                                      clocks: {
                                        black: blackClock,
                                        white: whiteClock,
                                      }
                                    }
                                  }""".stripMargin
                    )
                  ),
                  "orientation" -> "$setup.orientation",
                  "name"        -> true,
                  "lastMoveAt"  -> "$relay.lastMoveAt"
                )
              )
            )
        }
        .map: r =>
          for
            doc  <- r
            id   <- doc.getAsOpt[StudyChapterId]("_id")
            name <- doc.getAsOpt[StudyChapterName]("name")
            lastMoveAt = doc.getAsOpt[Instant]("lastMoveAt")
            comp   <- doc.getAsOpt[Bdoc]("comp")
            node   <- comp.getAsOpt[Bdoc]("node")
            fen    <- node.getAsOpt[Fen.Epd]("fen")
            clocks <- comp.getAsOpt[Bdoc]("clocks")
            lastMove   = node.getAsOpt[Uci]("uci")
            tags       = comp.getAsOpt[Tags]("tags")
            blackClock = clocks.getAsOpt[Centis]("black")
            whiteClock = clocks.getAsOpt[Centis]("white")
          yield ChapterPreview(
            id = id,
            name = name,
            players = tags flatMap ChapterPreview.players(blackClock = blackClock, whiteClock = whiteClock),
            orientation = doc.getAsOpt[Color]("orientation") | Color.White,
            fen = fen,
            lastMove = lastMove,
            lastMoveAt = lastMoveAt,
            playing = lastMove.isDefined && tags.flatMap(_(_.Result)).has("*"),
            outcome = tags.flatMap(_.outcome)
          )

  import lila.common.Json.{ writeAs, given }

  given Writes[ChapterPreview.Player] = Writes[ChapterPreview.Player] { p =>
    Json
      .obj("name" -> p.name)
      .add("title" -> p.title)
      .add("rating" -> p.rating)
      .add("clock" -> p.clock)
  }

  given Writes[ChapterPreview.Players] = Writes[ChapterPreview.Players] { players =>
    Json.obj("white" -> players.white, "black" -> players.black)
  }

  given Writes[Outcome] = writeAs(_.toString.replace("1/2", "½"))

  given Writes[ChapterPreview] = Json.writes

object StudyMultiBoard:

  case class ChapterPreview(
      id: StudyChapterId,
      name: StudyChapterName,
      players: Option[ChapterPreview.Players],
      orientation: Color,
      fen: Fen.Epd,
      lastMove: Option[Uci],
      lastMoveAt: Option[Instant],
      playing: Boolean,
      outcome: Option[Outcome]
  )

  object ChapterPreview:

    case class Player(name: String, title: Option[String], rating: Option[Int], clock: Option[Centis])

    type Players = ByColor[Player]

    def players(blackClock: Option[Centis], whiteClock: Option[Centis])(tags: Tags): Option[Players] =
      for
        wName <- tags(_.White)
        bName <- tags(_.Black)
      yield ByColor(
        white = Player(wName, tags(_.WhiteTitle), tags(_.WhiteElo).flatMap(_.toIntOption), whiteClock),
        black = Player(bName, tags(_.BlackTitle), tags(_.BlackElo).flatMap(_.toIntOption), blackClock)
      )
