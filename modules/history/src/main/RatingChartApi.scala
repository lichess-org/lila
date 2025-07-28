package lila.history

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.data.SafeJsonStr

final class RatingChartApi(
    historyApi: HistoryApi,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi
)(using Executor, lila.core.i18n.Translator):

  def apply[U: UserIdOf](user: U): Fu[Option[SafeJsonStr]] = cache.get(user.id)

  def singlePerf(user: User, perfKey: PerfKey): Fu[JsArray] =
    historyApi
      .ratingsMap(user, perfKey)
      .map(ratingsMapToJson(user.createdAt, _))
      .map(JsArray.apply)

  private val cache = cacheApi[UserId, Option[SafeJsonStr]](4096, "history.rating"):
    _.expireAfterWrite(10.minutes)
      .maximumSize(4096)
      .buildAsyncFuture(build)

  private def ratingsMapToJson(createdAt: Instant, ratingsMap: RatingsMap) =
    ratingsMap.map: (days, rating) =>
      val date = createdAt.plusDays(days).date
      Json.arr(date.getYear, date.getMonthValue - 1, date.getDayOfMonth, rating)

  private def build(userId: UserId): Fu[Option[SafeJsonStr]] =
    given Lang = lila.core.i18n.defaultLang
    userApi.createdAtById(userId).flatMapz { createdAt =>
      historyApi
        .get(userId)
        .map2: history =>
          RatingChartApi.perfTypes.map: pt =>
            Json.obj(
              "name" -> pt.trans,
              "points" -> ratingsMapToJson(createdAt, history(pt))
            )
        .map2(Json.toJson)
        .map2(lila.common.String.html.safeJsonValue)
    }

object RatingChartApi:

  import lila.rating.PerfType.*
  private val perfTypes = List(
    UltraBullet,
    Bullet,
    Blitz,
    Rapid,
    Classical,
    Correspondence,
    Crazyhouse,
    Chess960,
    KingOfTheHill,
    ThreeCheck,
    Antichess,
    Atomic,
    Horde,
    RacingKings,
    Puzzle
  )
