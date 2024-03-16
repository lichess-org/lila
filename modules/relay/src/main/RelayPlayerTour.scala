package lila.relay

import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.fide.FidePlayer
import lila.study.ChapterRepo

final class RelayPlayerTour(
    colls: RelayColls,
    chapterRepo: ChapterRepo,
    pager: RelayPager,
    cacheApi: lila.memo.CacheApi,
    playerApi: lila.fide.FidePlayerApi // for initial guessAndDenormalizeFideIds
)(using Executor, akka.stream.Materializer):

  private val tourIdsCache = cacheApi[chess.FideId, List[RelayTour.Id]](1024, "relay.player.tourIds"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture: fideId =>
      chapterRepo
        .studyIdsByRelayFideId(fideId)
        .flatMap: studyIds =>
          colls.round.distinctEasy[RelayTour.Id, List]("tourId", $inIds(studyIds))

  def playerTours(player: FidePlayer, page: Int): Fu[Paginator[RelayTour.WithLastRound]] =
    tourIdsCache
      .get(player.id)
      .flatMap:
        pager.byIds(_, page)
