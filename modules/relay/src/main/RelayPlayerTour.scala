package lila.relay

import scalalib.paginator.Paginator

import lila.core.fide.Player
import lila.db.dsl.{ *, given }
import lila.study.ChapterRepo

final class RelayPlayerTour(
    colls: RelayColls,
    chapterRepo: ChapterRepo,
    pager: RelayPager,
    cacheApi: lila.memo.CacheApi
)(using Executor, akka.stream.Materializer):

  private val tourIdsCache = cacheApi[chess.FideId, List[RelayTourId]](128, "relay.player.tourIds"):
    _.expireAfterWrite(10.minutes).buildAsyncFuture: fideId =>
      chapterRepo
        .studyIdsByRelayFideId(fideId)
        .flatMap: studyIds =>
          colls.round.distinctEasy[RelayTourId, List]("tourId", $inIds(studyIds))

  def playerTours(player: Player, page: Int): Fu[Paginator[RelayTour.WithLastRound]] =
    tourIdsCache
      .get(player.id)
      .flatMap:
        pager.byIds(_, page)
