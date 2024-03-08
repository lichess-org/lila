package lila.study

import BSONHandlers.given
import chess.{ ByColor, Centis, Color, Outcome, PlayerName, PlayerTitle, Elo }
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

  import ChapterPreview.*
  import ChapterPreview.bson.{ projection, given }
  import ChapterPreview.json.given

  def json(studyId: StudyId, page: Int, playing: Boolean): Fu[JsObject] = {
    if page == 1 && !playing then firstPageCache.get(studyId)
    else fetch(studyId, page, playing)
  }.map { PaginatorJson(_) }

  def invalidate(studyId: StudyId): Unit =
    firstPageCache.synchronous().invalidate(studyId)
    listCache.synchronous().invalidate(studyId)

  private val firstPageCache: AsyncLoadingCache[StudyId, Paginator[ChapterPreview]] =
    cacheApi.scaffeine
      .refreshAfterWrite(4 seconds)
      .expireAfterAccess(10 minutes)
      .buildAsyncFuture[StudyId, Paginator[ChapterPreview]] { fetch(_, 1, playing = false) }

  private val playingSelector = $doc("tags" -> "Result:*", "relay.path".$ne(""))

  private val listCache: AsyncLoadingCache[StudyId, JsValue] =
    cacheApi.scaffeine
      .expireAfterWrite(10 seconds)
      .buildAsyncFuture[StudyId, JsValue]: studyId =>
        fetch(studyId, 1, false, Study.maxChapters.into(MaxPerPage)).map: pager =>
          Json.toJson(pager.currentPageResults)

  def list(studyId: StudyId): Fu[JsValue] = listCache.get(studyId)

  def fetch(
      studyId: StudyId,
      page: Int,
      playing: Boolean,
      max: MaxPerPage = maxPerPage
  ): Fu[Paginator[ChapterPreview]] =
    chapterRepo.coll: coll =>
      Paginator[ChapterPreview](
        adapter = lila.db.paginator
          .Adapter[ChapterPreview](
            coll,
            selector = chapterRepo.$studyId(studyId) ++ playing.so(playingSelector),
            projection = projection.some,
            sort = chapterRepo.$sortOrder
          )
          .withNbResults(fuccess(Study.maxChapters.value)),
        currentPage = page,
        maxPerPage = max
      )
