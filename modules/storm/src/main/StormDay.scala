package lila.storm

import org.joda.time.DateTime
import org.joda.time.Days
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.Day
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User
import lila.user.UserRepo

// stores data of the best run of the day
// plus the number of runs
case class StormDay(
    _id: StormDay.Id,
    score: Int,
    moves: Int,
    errors: Int,
    combo: Int,
    time: Int,
    highest: Int,
    runs: Int
) {

  def add(run: StormForm.RunData) = {
    if (run.score > score)
      copy(
        score = run.score,
        moves = run.moves,
        errors = run.errors,
        combo = run.combo,
        time = run.time,
        highest = run.highest
      )
    else this
  }.copy(runs = runs + 1)

  def accuracyPercent: Float = 100 * (moves - errors) / moves.toFloat
}

object StormDay {

  case class Id(userId: User.ID, day: Day)
  object Id {
    def today(userId: User.ID)     = Id(userId, Day.today)
    def lastWeek(userId: User.ID)  = Id(userId, Day daysAgo 7)
    def lastMonth(userId: User.ID) = Id(userId, Day daysAgo 30)
    def allTime(userId: User.ID)   = Id(userId, Day(0))
  }

  def empty(id: Id) = StormDay(id, 0, 0, 0, 0, 0, 0, 0)
}

final class StormDayApi(coll: Coll, highApi: StormHighApi, userRepo: UserRepo, sign: StormSign)(implicit
    ctx: ExecutionContext
) {

  import StormDay._
  import StormBsonHandlers._

  def addRun(data: StormForm.RunData, user: Option[User]): Fu[Option[StormHigh.NewHigh]] = {
    lila.mon.storm.run.score(user.isDefined).record(data.score).unit
    user ?? { u =>
      if (sign.check(u, ~data.signed)) {
        Bus.publish(lila.hub.actorApi.storm.StormRun(u.id, data.score), "stormRun")
        highApi get u.id flatMap { prevHigh =>
          val todayId = Id today u.id
          coll
            .one[StormDay]($id(todayId))
            .map {
              _.getOrElse(StormDay empty todayId) add data
            }
            .flatMap { day =>
              coll.update.one($id(day._id), day, upsert = true)
            }
            .flatMap { _ =>
              val (high, newHigh) = highApi.update(u.id, prevHigh, data.score)
              userRepo.addStormRun(u.id, high.allTime.some.filter(prevHigh.allTime <)) inject newHigh
            }
        }
      } else {
        if (data.time > 40) {
          logger.warn(s"badly signed run from ${u.username} $data")
          lila.mon.storm.run.sign(data.signed match {
            case None              => "missing"
            case Some("")          => "empty"
            case Some("undefined") => "undefined"
            case _                 => "wrong"
          })
        }
        fuccess(none)
      }
    }
  }

  def history(userId: User.ID, page: Int): Fu[Paginator[StormDay]] =
    Paginator(
      adapter = new Adapter[StormDay](
        collection = coll,
        selector = $doc("_id" $startsWith s"${userId}:"),
        projection = none,
        sort = $sort desc "_id"
      ),
      page,
      MaxPerPage(30)
    )

}
