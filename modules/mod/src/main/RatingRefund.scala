package lila.mod

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.BSONHandlers._
import lila.game.{ Game, GameRepo, Query }
import lila.rating.PerfType
import lila.user.{ User, UserRepo }

private final class RatingRefund(scheduler: lila.common.Scheduler) {

  import RatingRefund._

  def schedule(cheater: User): Unit = scheduler.once(delay)(apply(cheater))

  private def apply(cheater: User): Unit = {
    println(s"Refunding ${cheater.username} victimes")
  }

  private def cheaterLastGames(cheater: User) = GameRepo.coll.find(
    Query.win(cheater.id) ++ Query.rated ++ Query.createdSince(DateTime.now minusDays 3)
  ).sort(Query.sortCreated)
    .cursor[Game](readPreference = ReadPreference.secondary)
    .list(30)
}

private object RatingRefund {

  val delay = 5 seconds
  // val delay = 1 minute

  case class Refund(victim: User, perf: PerfType, points: Int) {
    def is(v: User, p: PerfType): Boolean = v.id == victim.id && p == perf
    def is(r: Refund): Boolean = is(r.victim, r.perf)
    def add(p: Int) = copy(points = points + p)
  }

  case class Refunds(all: List[Refund]) {
    def add(victim: User, perf: PerfType, points: Int) = copy(all =
      all.find(_.is(victim, perf)) match {
        case None    => Refund(victim, perf, points) :: all
        case Some(r) => r.add(points) :: all.filterNot(_ is r)
      })
  }
}
