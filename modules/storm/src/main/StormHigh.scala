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

  def update(userId: User.ID, prev: StormHigh, score: Int): (StormHigh, Option[NewHigh]) = {
    val newHigh = prev update score
    newHigh -> {
      (newHigh != prev) ?? {
        cache.put(userId, fuccess(newHigh))
        import NewHigh._
        if (newHigh.allTime > prev.allTime) AllTime(prev.allTime).some
        else if (newHigh.month > prev.month) Month(prev.month).some
        else if (newHigh.week > prev.week) Week(prev.week).some
        else Day(prev.day).some
      }
    }
  }

  private val cache = cacheApi[User.ID, StormHigh](8192, "storm.high") {
    _.expireAfterAccess(1 hour).buildAsyncFuture(compute)
  }

  private def compute(userId: User.ID): Fu[StormHigh] =
    coll
      .aggregateOne() { framework =>
        import framework._
        val todayId = StormDay.Id today userId
        val project = Project($doc("_id" -> false, "score" -> true))
        def bestSince(sinceId: StormDay.Id) = List(
          Match($doc("_id" $lte todayId $gt sinceId)),
          Sort(Descending("score")),
          Limit(1),
          project
        )
        Facet(
          List(
            "day"     -> List(Match($id(todayId)), project),
            "week"    -> bestSince(StormDay.Id.lastWeek(userId)),
            "month"   -> bestSince(StormDay.Id.lastMonth(userId)),
            "allTime" -> bestSince(StormDay.Id.allTime(userId))
          )
        ) -> Nil
      }
      .map2 { doc =>
        println(lila.db.BSON.debug(doc))
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
