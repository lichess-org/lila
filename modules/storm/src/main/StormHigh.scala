package lila.storm

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

case class StormHigh(day: Int, week: Int, month: Int, allTime: Int) {

  def update(score: Int) = copy(
    day = day atLeast score,
    week = week atLeast score,
    month = month atLeast score,
    allTime = allTime atLeast score
  )
}

object StormHigh {
  val default = StormHigh(0, 0, 0, 0)

  sealed abstract class NewHigh(val key: String) {
    val previous: Int
  }
  object NewHigh {
    case class Day(previous: Int)     extends NewHigh("day")
    case class Week(previous: Int)    extends NewHigh("week")
    case class Month(previous: Int)   extends NewHigh("month")
    case class AllTime(previous: Int) extends NewHigh("allTime")
  }
}

final class StormHighApi(coll: Coll, cacheApi: CacheApi)(implicit ctx: ExecutionContext) {

  import StormBsonHandlers._
  import StormHigh.NewHigh

  def get(userId: User.ID): Fu[StormHigh] = cache get userId

  def update(userId: User.ID, prev: StormHigh, score: Int): Option[NewHigh] = {
    val high = prev update score
    (high != prev) ?? {
      cache.put(userId, fuccess(high))
      import NewHigh._
      if (high.allTime > prev.allTime) AllTime(prev.allTime).some
      else if (high.month > prev.month) Month(prev.month).some
      else if (high.week > prev.week) Week(prev.week).some
      else Day(prev.day).some
    }
  }

  private val cache = cacheApi[User.ID, StormHigh](8192, "storm.high") {
    _.expireAfterAccess(1 hour).buildAsyncFuture(compute)
  }

  private def compute(userId: User.ID): Fu[StormHigh] =
    coll
      .aggregateOne() { framework =>
        import framework._
        def matchSince(sinceId: User.ID => StormDay.Id) = Match($doc("_id" $gte sinceId(userId)))
        val scoreSort                                   = Sort(Descending("score"))
        Match($doc("_id" $lte StormDay.Id.today(userId) $gt StormDay.Id.allTime(userId))) -> List(
          Project($doc("score" -> true)),
          Sort(Descending("_id")),
          Facet(
            List(
              "day"     -> List(Limit(1), matchSince(StormDay.Id.today)),
              "week"    -> List(Limit(7), matchSince(StormDay.Id.lastWeek), scoreSort, Limit(1)),
              "month"   -> List(Limit(30), matchSince(StormDay.Id.lastMonth), scoreSort, Limit(1)),
              "allTime" -> List(scoreSort, Limit(1))
            )
          )
        )
      }
      .map2 { doc =>
        def readScore(doc: Bdoc, field: String) =
          ~doc.getAsOpt[List[Bdoc]](field).flatMap(_.headOption).flatMap(_.getAsOpt[Int]("score"))
        StormHigh(
          day = readScore(doc, "day"),
          week = readScore(doc, "week"),
          month = readScore(doc, "month"),
          allTime = readScore(doc, "allTime")
        )
      }
      .map(_ | StormHigh.default)
}
