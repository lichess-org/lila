package lila.storm

import org.joda.time.DateTime
import org.joda.time.Days
import scala.concurrent.ExecutionContext

import lila.common.Day
import lila.db.dsl._
import lila.user.User

case class StormDay(
    _id: StormDay.Id,
    runs: Int,
    score: Int,
    moves: Int,
    combo: Int,
    time: Int,
    highest: Int
) {

  def add(run: StormForm.RunData) = copy(
    runs = runs + 1,
    score = score atLeast run.score,
    moves = moves atLeast run.moves,
    combo = combo atLeast run.combo,
    time = time atLeast run.time,
    highest = highest atLeast run.highest
  )
}

object StormDay {

  case class Id(userId: User.ID, day: Day)
  object Id {
    def today(userId: User.ID)     = Id(userId, Day.today)
    def lastWeek(userId: User.ID)  = Id(userId, Day daysAgo 7)
    def lastMonth(userId: User.ID) = Id(userId, Day daysAgo 30)
    def allTime(userId: User.ID)   = Id(userId, Day(0))
  }

  def empty(id: Id) = StormDay(id, 0, 0, 0, 0, 0, 0)
}

final class StormDayApi(coll: Coll, highApi: StormHighApi)(implicit ctx: ExecutionContext) {

  import StormDay._
  import StormBsonHandlers._

  def addRun(data: StormForm.RunData, user: Option[User]): Fu[Option[StormHigh.NewHigh]] = {
    lila.mon.storm.run.score(user.isDefined).record(data.score).unit
    user ?? { u =>
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
          .void inject highApi.update(u.id, prevHigh, data.score)
      }
    }
  }
}
