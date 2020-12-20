package lila.history

import scala.concurrent.duration._

import play.api.libs.json._

import lila.rating.PerfType
import lila.user.User

final class RatingChartApi(
    historyApi: HistoryApi,
    mongoCache: lila.memo.MongoCache.Api
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(user: User): Fu[Option[String]] =
    cache.get(user) dmap { chart =>
      chart.nonEmpty option chart
    }

  def singlePerf(user: User, perfType: PerfType): Fu[JsArray] =
    historyApi.ratingsMap(user, perfType) map {
      ratingsMapToJson(user, _)
    } map JsArray.apply

  private val cache = mongoCache[User, String](
    1024,
    "history:rating",
    60 minutes,
    _.id
  ) { loader =>
    _.expireAfterAccess(10 minutes)
      .maximumSize(2048)
      .buildAsyncFuture {
        loader { user =>
          build(user) dmap (~_)
        }
      }
  }

  private def ratingsMapToJson(user: User, ratingsMap: RatingsMap) =
    ratingsMap.map {
      case (days, rating) =>
        val date = user.createdAt plusDays days
        Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
    }

  private def build(user: User): Fu[Option[String]] =
    historyApi get user.id map2 { (history: History) =>
      lila.common.String.html.safeJsonValue {
        Json.toJson {
          import lila.rating.PerfType._
          List(
            Bullet,
            Blitz,
            Rapid,
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
          ) map { pt =>
            Json.obj(
              "name"   -> pt.trans(lila.i18n.defaultLang),
              "points" -> ratingsMapToJson(user, history(pt))
            )
          }
        }
      }
    }
}
