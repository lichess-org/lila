package lila.history

import play.api.libs.json._
import scala.concurrent.duration._

import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import org.joda.time.DateTime

final class RatingChartApi(
    historyApi: HistoryApi,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(user: User): Fu[Option[String]] =
    cache.get(user.id) dmap { chart =>
      chart.nonEmpty option chart
    }

  def singlePerf(user: User, perfType: PerfType): Fu[JsArray] =
    historyApi.ratingsMap(user, perfType) map {
      ratingsMapToJson(user.id, user.createdAt, _)
    } map JsArray.apply

  private val cache = cacheApi[User.ID, String](4096, "history.rating") {
    _.expireAfterAccess(10 minutes)
      .maximumSize(4096)
      .buildAsyncFuture { userId =>
        build(userId).dmap(~_)
      }
  }

  private def ratingsMapToJson(userId: User.ID, createdAt: DateTime, ratingsMap: RatingsMap) =
    ratingsMap.map { case (days, rating) =>
      val date = createdAt plusDays days
      Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
    }

  private def build(userId: User.ID): Fu[Option[String]] =
    userRepo.createdAtById(userId) flatMap {
      _ ?? { createdAt =>
        historyApi get userId map2 { (history: History) =>
          lila.common.String.html.safeJsonValue {
            Json.toJson {
              RatingChartApi.perfTypes map { pt =>
                Json.obj(
                  "name"   -> pt.trans(lila.i18n.defaultLang),
                  "points" -> ratingsMapToJson(userId, createdAt, history(pt))
                )
              }
            }
          }
        }
      }
    }
}

object RatingChartApi {

  def bestPerfIndex(user: User): Int = user.bestPerf ?? { perfTypes indexOf _ }

  import lila.rating.PerfType._
  private val perfTypes = List(
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
  )
}
