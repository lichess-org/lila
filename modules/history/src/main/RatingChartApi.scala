package lila.history

import play.api.libs.json.*

import lila.common.Json.given
import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import play.api.i18n.Lang

final class RatingChartApi(
    historyApi: HistoryApi,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor, lila.core.i18n.Translator):

  def apply(user: User): Fu[Option[SafeJsonStr]] = cache.get(user.id)

  def singlePerf(user: User, perfType: PerfType): Fu[JsArray] =
    historyApi
      .ratingsMap(user, perfType)
      .map(ratingsMapToJson(user.createdAt, _))
      .map(JsArray.apply)

  private val cache = cacheApi[UserId, Option[SafeJsonStr]](4096, "history.rating"):
    _.expireAfterWrite(10 minutes)
      .maximumSize(4096)
      .buildAsyncFuture(build)

  private def ratingsMapToJson(createdAt: Instant, ratingsMap: RatingsMap) =
    ratingsMap.map: (days, rating) =>
      val date = createdAt.plusDays(days).date
      Json.arr(date.getYear, date.getMonthValue - 1, date.getDayOfMonth, rating)

  private def build(userId: UserId): Fu[Option[SafeJsonStr]] =
    given Lang = lila.core.i18n.defaultLang
    userRepo.createdAtById(userId).flatMapz { createdAt =>
      historyApi
        .get(userId)
        .map2: history =>
          RatingChartApi.perfTypes.map: pt =>
            Json.obj(
              "name"   -> pt.trans,
              "points" -> ratingsMapToJson(createdAt, history(pt))
            )
        .map2(Json.toJson)
        .map2(lila.common.String.html.safeJsonValue)
    }

object RatingChartApi:

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
