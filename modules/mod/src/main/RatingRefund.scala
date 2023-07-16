package lila.mod

import lila.db.dsl.*
import lila.game.BSONHandlers.given
import lila.game.{ Game, GameRepo, Query }
import lila.perfStat.PerfStat
import lila.rating.PerfType
import lila.report.{ Suspect, Victim }
import lila.user.{ User, UserApi, RankingApi, UserRepo, UserPerfsRepo }

final private class RatingRefund(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    perfsRepo: UserPerfsRepo,
    userApi: UserApi,
    scheduler: Scheduler,
    notifier: ModNotifier,
    historyApi: lila.history.HistoryApi,
    rankingApi: RankingApi,
    logApi: ModlogApi,
    perfStat: lila.perfStat.PerfStatApi
)(using Executor):

  import RatingRefund.*

  def schedule(sus: Suspect): Unit = scheduler.scheduleOnce(delay)(apply(sus))

  private def apply(sus: Suspect): Funit =
    logApi.wasUnengined(sus) flatMap {
      if _ then funit
      else
        def lastGames =
          gameRepo.coll
            .find:
              Query.user(sus.user.id) ++ Query.rated ++ Query
                .createdSince(nowInstant minusDays 3) ++ Query.finished
            .sort(Query.sortCreated)
            .cursor[Game](ReadPref.sec)
            .list(40)

        def makeRefunds(games: List[Game]) =
          games.foldLeft(Refunds(List.empty)): (refs, g) =>
            (for
              op <- g.opponentOf(sus.user)
              if op.provisional.no
              victim <- op.userId
              diff   <- op.ratingDiff
              if diff < 0
              rating <- op.rating
            yield refs.add(victim, g.perfType, -diff, rating)) | refs

        def pointsToRefund(ref: Refund, curRating: IntRating, stats: PerfStat): Int = {
          ref.diff.value - (ref.diff.value + curRating.value - ref.topRating.value atLeast 0) / 2 atMost
            stats.highest.fold(100)(_.int.value - curRating.value + 20)
        }.squeeze(0, 150)

        def refundPoints(victim: User.WithPerf, pt: PerfType, points: Int): Funit =
          val newPerf = victim.perf refund points
          perfsRepo.setPerf(victim.id, pt, newPerf) >>
            historyApi.setPerfRating(victim.user, pt, newPerf.intRating) >>
            rankingApi.save(victim.user, pt, newPerf) >>
            notifier.refund(victim.user, pt, points)

        def applyRefund(ref: Refund) =
          userApi.withPerf(ref.victim, ref.perf) flatMapz { user =>
            perfStat.get(user.user, ref.perf) flatMap { stats =>
              val points = pointsToRefund(ref, curRating = user.perf.intRating, stats = stats)
              (points > 0) so refundPoints(user, ref.perf, points)
            }
          }

        lastGames map makeRefunds flatMap { _.all.map(applyRefund).parallel } void
    }

private object RatingRefund:

  val delay = 1 minute

  case class Refund(victim: UserId, perf: PerfType, diff: IntRatingDiff, topRating: IntRating):
    def is(v: UserId, p: PerfType): Boolean = v == victim && p == perf
    def is(r: Refund): Boolean              = is(r.victim, r.perf)
    def add(d: IntRatingDiff, r: IntRating) = copy(diff = diff + d, topRating = topRating atLeast r)

  case class Refunds(all: List[Refund]):
    def add(victim: UserId, perf: PerfType, diff: IntRatingDiff, rating: IntRating) =
      copy(all = all.find(_.is(victim, perf)) match
        case None    => Refund(victim, perf, diff, rating) :: all
        case Some(r) => r.add(diff, rating) :: all.filterNot(_ is r)
      )
