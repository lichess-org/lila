package lila.history

import scala.concurrent.duration._

import play.api.libs.json._

import lila.rating.PerfType
import lila.user.User

final class RatingChartApi(
    historyApi: HistoryApi,
    mongoCache: lila.memo.MongoCache.Builder,
    cacheTtl: FiniteDuration
) {

  def apply(user: User): Fu[Option[String]] = cache(user) map { chart =>
    chart.nonEmpty option chart
  }

  def singlePerf(user: User, perfType: PerfType): Fu[JsArray] =
    historyApi.ratingsMap(user, perfType) map {
      ratingsMapToJson(user, _)
    } map JsArray.apply

  private val cache = mongoCache[User, String](
    prefix = "history:rating",
    f = user => build(user) map (~_),
    maxCapacity = 1024,
    timeToLive = cacheTtl,
    keyToString = _.id
  )

  private def ratingsMapToJson(user: User, ratingsMap: RatingsMap) = ratingsMap.map {
    case (days, rating) =>
      val date = user.createdAt plusDays days
      Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
  }

  private def build(user: User): Fu[Option[String]] = {
    val nbEstablishedPerfs = user.perfs.perfs.count(_._2.established)
    historyApi get user.id map2 { (history: History) =>
      Json stringify {
        Json.toJson {
          perfTypes collect {
            case pt if nbEstablishedPerfs < 3 || user.perfs(pt).established =>
              Json.obj(
                "name" -> pt.name,
                "points" -> ratingsMapToJson(user, history(pt))
              )
          }
        }
      }
    }
  }

  private val perfTypes = {
    import lila.rating.PerfType._
    List(
      Bullet,
      Blitz,
      Classical,
      Correspondence,
      Chess960,
      KingOfTheHill,
      ThreeCheck,
      Antichess,
      Atomic,
      Horde,
      RacingKings,
      Crazyhouse,
      Puzzle,
      UltraBullet
    )
  }
}
