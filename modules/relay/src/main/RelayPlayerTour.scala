package lila.relay

import lila.study.ChapterRepo
import lila.fide.FidePlayer
import lila.db.dsl.{ *, given }
import lila.common.paginator.Paginator

final class RelayPlayerTour(
    colls: RelayColls,
    chapterRepo: ChapterRepo,
    pager: RelayPager,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private val tourIdsCache = cacheApi[chess.FideId, List[RelayTour.Id]](1024, "relay.player.tourIds"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture: fideId =>
      chapterRepo
        .studyIdsByRelayFideId(fideId)
        .flatMap: studyIds =>
          colls.round.distinctEasy[RelayTour.Id, List]("tourId", $inIds(studyIds))

  def playerTours(player: FidePlayer): Fu[Paginator[RelayTour.WithLastRound]] =
    tourIdsCache.get(player.id) flatMap:
      pager.byIds(_, page = 1)
