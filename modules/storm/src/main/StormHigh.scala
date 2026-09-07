package lila.storm

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

case class StormHigh(day: Int, week: Int, month: Int, allTime: Int):

  def update(score: Int) = copy(
    day = day.atLeast(score),
    week = week.atLeast(score),
    month = month.atLeast(score),
    allTime = allTime.atLeast(score)
  )

object StormHigh:
  val default = StormHigh(0, 0, 0, 0)

  enum NewHigh(val key: String):
    val previous: Int
    case Day(previous: Int) extends NewHigh("day")
    case Week(previous: Int) extends NewHigh("week")
    case Month(previous: Int) extends NewHigh("month")
    case AllTime(previous: Int) extends NewHigh("allTime")

final class StormHighApi(coll: Coll, cacheApi: CacheApi)(using Executor):

  import StormBsonHandlers.given
  import StormHigh.NewHigh

  private val cache = cacheApi[UserId, StormHigh](16_384, "storm.high"):
    _.expireAfterAccess(30.minutes).buildAsyncFuture(compute)

  export cache.get

  private[storm] def update(userId: UserId, prev: StormHigh, score: Int): Option[NewHigh] =
    val high = prev.update(score)
    (high != prev).so:
      cache.put(userId, fuccess(high))
      import NewHigh.*
      if high.allTime > prev.allTime then AllTime(prev.allTime).some
      else if high.month > prev.month then Month(prev.month).some
      else if high.week > prev.week then Week(prev.week).some
      else Day(prev.day).some

  private def compute(userId: UserId): Fu[StormHigh] =
    coll
      .aggregateOne(_.sec): framework =>
        import framework.*
        def matchSince(sinceId: UserId => StormDay.Id) = Match($doc("_id".$gte(sinceId(userId))))
        val scoreSort = Sort(Descending("score"))
        Match($doc("_id".$lte(StormDay.Id.today(userId)).$gt(StormDay.Id.allTime(userId)))) -> List(
          Project($doc("score" -> true)),
          Sort(Descending("_id")),
          Facet(
            List(
              "day" -> List(Limit(1), matchSince(StormDay.Id.today)),
              "week" -> List(Limit(7), matchSince(StormDay.Id.lastWeek), scoreSort, Limit(1)),
              "month" -> List(Limit(30), matchSince(StormDay.Id.lastMonth), scoreSort, Limit(1)),
              "allTime" -> List(scoreSort, Limit(1))
            )
          )
        )
      .map2: doc =>
        def readScore(doc: Bdoc, field: String) =
          ~doc.getAsOpt[List[Bdoc]](field).flatMap(_.headOption).flatMap(_.getAsOpt[Int]("score"))
        StormHigh(
          day = readScore(doc, "day"),
          week = readScore(doc, "week"),
          month = readScore(doc, "month"),
          allTime = readScore(doc, "allTime")
        )
      .dmap(_ | StormHigh.default)
