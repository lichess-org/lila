package lila.mod

import reactivemongo.api.ReadPreference

import lila.db.dsl.*
import lila.game.BSONHandlers.given
import lila.game.{ Game, GameRepo, Query }
import lila.perfStat.PerfStat
import lila.rating.PerfType
import lila.report.{ Suspect, Victim }
import lila.user.UserRepo

final private class RatingRefund(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    scheduler: Scheduler,
    notifier: ModNotifier,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    logApi: ModlogApi,
    perfStat: lila.perfStat.PerfStatApi
)(using Executor):

  import RatingRefund.*

  def schedule(sus: Suspect): Unit = scheduler.scheduleOnce(delay)(apply(sus).unit).unit

  private def apply(sus: Suspect): Funit =
    logApi.wasUnengined(sus) flatMap {
      if _ then funit
      else
        def lastGames =
          gameRepo.coll
            .find(
              Query.user(sus.user.id) ++ Query.rated ++ Query
                .createdSince(nowInstant minusDays 3) ++ Query.finished
            )
            .sort(Query.sortCreated)
            .cursor[Game](ReadPreference.secondaryPreferred)
            .list(40)

        def makeRefunds(games: List[Game]) =
          games.foldLeft(Refunds(List.empty)) { case (refs, g) =>
            (for {
              perf <- g.perfType
              op   <- g.playerByUserId(sus.user.id) map g.opponent
              if op.provisional.no
              victim <- op.userId
              diff   <- op.ratingDiff
              if diff < 0
              rating <- op.rating
            } yield refs.add(victim, perf, -diff, rating)) | refs
          }

        def pointsToRefund(ref: Refund, curRating: IntRating, perfs: PerfStat): Int = {
          ref.diff.value - (ref.diff + curRating - ref.topRating atLeast 0).value / 2 atMost
            perfs.highest.fold(100) { _.int.value - curRating.value + 20 }
        }.squeeze(0, 150)

        def refundPoints(victim: Victim, pt: PerfType, points: Int): Funit =
          val newPerf = victim.user.perfs(pt) refund points
          userRepo.setPerf(victim.user.id, pt, newPerf) >>
            historyApi.setPerfRating(victim.user, pt, newPerf.intRating) >>
            rankingApi.save(victim.user, pt, newPerf) >>
            notifier.refund(victim, pt, points)

        def applyRefund(ref: Refund) =
          userRepo byId ref.victim flatMapz { user =>
            perfStat.get(user, ref.perf) flatMap { perfs =>
              val points = pointsToRefund(
                ref,
                curRating = user.perfs(ref.perf).intRating,
                perfs = perfs
              )
              (points > 0) ?? refundPoints(Victim(user), ref.perf, points)
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
      copy(all = all.find(_.is(victim, perf)) match {
        case None    => Refund(victim, perf, diff, rating) :: all
        case Some(r) => r.add(diff, rating) :: all.filterNot(_ is r)
      })
