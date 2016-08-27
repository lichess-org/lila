package lila.mod

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.BSONHandlers._
import lila.game.{ Game, GameRepo, Query }
import lila.rating.PerfType
import lila.user.{ User, UserRepo }

private final class RatingRefund(
    scheduler: lila.common.Scheduler,
    notifier: ModNotifier,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    wasUnengined: User.ID => Fu[Boolean]) {

  import RatingRefund._

  def schedule(cheater: User): Unit = scheduler.once(delay)(apply(cheater))

  private def apply(cheater: User): Unit = wasUnengined(cheater.id) flatMap {
    case true => funit
    case false =>

      logger.info(s"Refunding ${cheater.username} victims")

      def lastGames = GameRepo.coll.find(
        Query.win(cheater.id) ++ Query.rated ++ Query.createdSince(DateTime.now minusDays 3)
      ).sort(Query.sortCreated)
        .cursor[Game](readPreference = ReadPreference.secondaryPreferred)
        .list(30)

      def opponent(game: Game) = game.playerByUserId(cheater.id) map game.opponent

      def makeRefunds(games: List[Game]) = games.foldLeft(Refunds(List.empty)) {
        case (refs, g) => (for {
          perf <- g.perfType
          op <- g.playerByUserId(cheater.id) map g.opponent
          if !op.provisional
          victim <- op.userId
          diff <- op.ratingDiff
          if diff < 0
          rating <- op.rating
        } yield refs.add(victim, perf, -diff, rating)) | refs
      }

      def pointsToRefund(ref: Refund, user: User): Int = {
        ref.diff - user.perfs(ref.perf).intRating + ref.topRating
      } min ref.diff min 200 max 0

      def refundPoints(user: User, pt: PerfType, points: Int): Funit = {
        val newPerf = user.perfs(pt) refund points
        UserRepo.setPerf(user.id, pt, newPerf) >>
          historyApi.setPerfRating(user, pt, newPerf.intRating) >>
          rankingApi.save(user.id, pt, newPerf) >>
          notifier.refund(user, pt, points)
      }

      def applyRefund(ref: Refund) =
        UserRepo byId ref.victim flatMap {
          _ ?? { user =>
            val points = pointsToRefund(ref, user)
            logger.info(s"Refunding $ref -> $points")
            (points > 0) ?? refundPoints(user, ref.perf, points)
          }
        }

      lastGames map makeRefunds flatMap { _.all.map(applyRefund).sequenceFu } void
  }
}

private object RatingRefund {

  val delay = 1 minute

  case class Refund(victim: User.ID, perf: PerfType, diff: Int, topRating: Int) {
    def is(v: User.ID, p: PerfType): Boolean = v == victim && p == perf
    def is(r: Refund): Boolean = is(r.victim, r.perf)
    def add(d: Int, r: Int) = copy(diff = diff + d, topRating = topRating max r)
  }

  case class Refunds(all: List[Refund]) {
    def add(victim: User.ID, perf: PerfType, diff: Int, rating: Int) = copy(all =
      all.find(_.is(victim, perf)) match {
        case None    => Refund(victim, perf, diff, rating) :: all
        case Some(r) => r.add(diff, rating) :: all.filterNot(_ is r)
      })
  }
}
