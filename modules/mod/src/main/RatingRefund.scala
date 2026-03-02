package lila.mod

import chess.IntRating
import chess.rating.IntRatingDiff

import lila.core.game.GameRepo
import lila.core.user.WithPerf
import lila.db.dsl.*
import lila.game.Query
import lila.rating.PerfExt.refund
import lila.rating.PerfType
import lila.report.Suspect
import lila.user.{ RankingApi, UserApi, UserPerfsRepo }

final private class RatingRefund(
    gameRepo: GameRepo,
    perfsRepo: UserPerfsRepo,
    userApi: UserApi,
    scheduler: Scheduler,
    notifier: ModNotifier,
    historyApi: lila.core.history.HistoryApi,
    rankingApi: RankingApi,
    logApi: ModlogApi,
    perfStat: lila.core.perf.PerfStatApi
)(using Executor):

  import RatingRefund.*
  import gameRepo.gameHandler

  def schedule(sus: Suspect): Unit = scheduler.scheduleOnce(delay)(apply(sus))

  private def apply(sus: Suspect): Funit =
    val refundDateLimit = nowInstant.minusDays(5)
    logApi
      .wasUnengined(sus, refundDateLimit.some)
      .flatMap:
        if _ then funit
        else
          def lastGames =
            gameRepo.coll
              .find:
                Query.user(sus.user.id) ++ Query.rated ++ Query
                  .createdSince(refundDateLimit) ++ Query.finished
              .sort(Query.sortCreated)
              .cursor[Game](ReadPref.sec)
              .list(40)

          def makeRefunds(games: List[Game]) =
            games.foldLeft(Refunds(List.empty)): (refs, g) =>
              (for
                op <- g.opponentOf(sus.user)
                if op.provisional.no
                victim <- op.userId
                diff <- op.ratingDiff
                if diff < IntRatingDiff(0)
                rating <- op.rating
              yield refs.add(victim, g.perfKey, -diff, rating)) | refs

          def pointsToRefund(ref: Refund, curRating: IntRating, highest: Option[IntRating]): Int = {
            (ref.diff.value - ((ref.diff.value + curRating.value - ref.topRating.value).atLeast(0)) / 2)
              .atMost(highest.fold(100)(_.value - curRating.value + 20))
          }.squeeze(0, 150)

          def refundPoints(victim: WithPerf, pt: PerfType, points: Int): Funit =
            val newPerf = victim.perf.refund(points)
            perfsRepo.setPerf(victim.id, pt, newPerf) >>
              historyApi.setPerfRating(victim.user, pt, newPerf.intRating) >>
              rankingApi.save(victim.user, pt, newPerf) >>
              notifier.refund(victim.user, pt, points)

          def applyRefund(ref: Refund) =
            userApi.byIdWithPerf(ref.victim, ref.perf).flatMapz { user =>
              perfStat.highestRating(user.user.id, ref.perf).flatMap { highest =>
                val points = pointsToRefund(ref, curRating = user.perf.intRating, highest = highest)
                (points > 0).so(refundPoints(user, ref.perf, points))
              }
            }

          lastGames.map(makeRefunds).flatMap(_.all.parallelVoid(applyRefund))

private object RatingRefund:

  val delay = 1.minute

  case class Refund(victim: UserId, perf: PerfKey, diff: IntRatingDiff, topRating: IntRating):
    def is(v: UserId, p: PerfKey): Boolean = v == victim && p == perf
    def is(r: Refund): Boolean = is(r.victim, r.perf)
    def add(d: IntRatingDiff, r: IntRating) = copy(diff = diff + d, topRating = topRating.atLeast(r))

  case class Refunds(all: List[Refund]):
    def add(victim: UserId, perf: PerfKey, diff: IntRatingDiff, rating: IntRating) =
      copy(all = all.find(_.is(victim, perf)) match
        case None => Refund(victim, perf, diff, rating) :: all
        case Some(r) => r.add(diff, rating) :: all.filterNot(_.is(r)))
