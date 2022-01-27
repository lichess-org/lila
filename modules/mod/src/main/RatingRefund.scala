package lila.mod

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.BSONHandlers._
import lila.game.{ Game, GameRepo, Query }
import lila.perfStat.PerfStat
import lila.rating.PerfType
import lila.report.{ Suspect, Victim }
import lila.user.{ User, UserRepo }

final private class RatingRefund(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    scheduler: akka.actor.Scheduler,
    notifier: ModNotifier,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    logApi: ModlogApi,
    perfStat: lila.perfStat.PerfStatApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import RatingRefund._

  def schedule(sus: Suspect): Unit = scheduler.scheduleOnce(delay)(apply(sus).unit).unit

  private def apply(sus: Suspect): Funit =
    logApi.wasUnengined(sus) flatMap {
      case true => funit
      case false =>
        def lastGames =
          gameRepo.coll
            .find(
              Query.user(sus.user.id) ++ Query.rated ++ Query
                .createdSince(DateTime.now minusDays 3) ++ Query.finished
            )
            .sort(Query.sortCreated)
            .cursor[Game](ReadPreference.secondaryPreferred)
            .list(40)

        def makeRefunds(games: List[Game]) =
          games.foldLeft(Refunds(List.empty)) { case (refs, g) =>
            (for {
              perf <- g.perfType
              op   <- g.playerByUserId(sus.user.id) map g.opponent
              if !op.provisional
              victim <- op.userId
              diff   <- op.ratingDiff
              if diff < 0
              rating <- op.rating
            } yield refs.add(victim, perf, -diff, rating)) | refs
          }

        def pointsToRefund(ref: Refund, curRating: Int, perfs: PerfStat): Int = {
          ref.diff - (ref.diff + curRating - ref.topRating atLeast 0) / 2 atMost
            perfs.highest.fold(100) { _.int - curRating + 20 }
        }.squeeze(0, 150)

        def refundPoints(victim: Victim, pt: PerfType, points: Int): Funit = {
          val newPerf = victim.user.perfs(pt) refund points
          userRepo.setPerf(victim.user.id, pt, newPerf) >>
            historyApi.setPerfRating(victim.user, pt, newPerf.intRating) >>
            rankingApi.save(victim.user, pt, newPerf) >>
            notifier.refund(victim, pt, points)
        }

        def applyRefund(ref: Refund) =
          userRepo byId ref.victim flatMap {
            _ ?? { user =>
              perfStat.get(user, ref.perf) flatMap { perfs =>
                val points = pointsToRefund(
                  ref,
                  curRating = user.perfs(ref.perf).intRating,
                  perfs = perfs
                )
                (points > 0) ?? refundPoints(Victim(user), ref.perf, points)
              }
            }
          }

        lastGames map makeRefunds flatMap { _.all.map(applyRefund).sequenceFu } void
    }
}

private object RatingRefund {

  val delay = 1 minute

  case class Refund(victim: User.ID, perf: PerfType, diff: Int, topRating: Int) {
    def is(v: User.ID, p: PerfType): Boolean = v == victim && p == perf
    def is(r: Refund): Boolean               = is(r.victim, r.perf)
    def add(d: Int, r: Int)                  = copy(diff = diff + d, topRating = topRating max r)
  }

  case class Refunds(all: List[Refund]) {
    def add(victim: User.ID, perf: PerfType, diff: Int, rating: Int) =
      copy(all = all.find(_.is(victim, perf)) match {
        case None    => Refund(victim, perf, diff, rating) :: all
        case Some(r) => r.add(diff, rating) :: all.filterNot(_ is r)
      })
  }
}
