package lila.history

import play.api.libs.json.*
import scala.concurrent.duration.*

import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import org.joda.time.DateTime
import lila.common.Json.given

final class RatingChartApi(
    historyApi: HistoryApi,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi
)(using ec: scala.concurrent.ExecutionContext):

  def apply(user: User): Fu[Option[String]] =
    cache.get(user.id) dmap { chart =>
      chart.nonEmpty option chart
    }

  def singlePerf(user: User, perfType: PerfType): Fu[JsArray] =
    historyApi.ratingsMap(user, perfType) map {
      ratingsMapToJson(user.id, user.createdAt, _)
    } map JsArray.apply

  private val cache = cacheApi[UserId, String](4096, "history.rating") {
    _.expireAfterWrite(10 minutes)
      .maximumSize(4096)
      .buildAsyncFuture { userId =>
        build(userId).dmap(~_)
      }
  }

  private def ratingsMapToJson(userId: UserId, createdAt: DateTime, ratingsMap: RatingsMap) =
    ratingsMap.map { case (days, rating) =>
      val date = createdAt plusDays days
      Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
    }

  private def build(userId: UserId): Fu[Option[String]] =
    userRepo.createdAtById(userId) flatMap {
      _ ?? { createdAt =>
        historyApi get userId map2 { (history: History) =>
          lila.common.String.html.safeJsonValue {
            Json.toJson {
              RatingChartApi.perfTypes map { pt =>
                Json.obj(
                  "name"   -> pt.trans(using lila.i18n.defaultLang),
                  "points" -> ratingsMapToJson(userId, createdAt, history(pt))
                )
              }
            }
          }
        }
      }
    }

object RatingChartApi:

  def bestPerfIndex(user: User): Int = user.bestPerf ?? { perfTypes indexOf _ }

  import lila.rating.PerfType.*
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
