package lila.history

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.data.SafeJsonStr
import cats.data.OptionT

final class RatingChartApi(
    historyApi: HistoryApi,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  def apply[U: UserIdOf](user: U, computeIfNeeded: Boolean): Fu[Option[SafeJsonStr]] =
    if computeIfNeeded then cache.get(user.id) else ~cache.getIfPresent(user.id)

  def singlePerf(user: User, perfKey: PerfKey): Fu[JsArray] =
    historyApi
      .ratingsMap(user, perfKey)
      .map(ratingsMapToJson(user.createdAt, _))
      .map(JsArray.apply)

  private val cache = cacheApi[UserId, Option[SafeJsonStr]](8192, "history.rating"):
    _.expireAfterWrite(10.minutes)
      .maximumSize(8192)
      .buildAsyncFuture(build)

  private def ratingsMapToJson(createdAt: Instant, ratingsMap: RatingsMap) =
    ratingsMap.map: (days, rating) =>
      val date = createdAt.plusDays(days).date
      Json.arr(date.getYear, date.getMonthValue - 1, date.getDayOfMonth, rating)

  private def build(userId: UserId): Fu[Option[SafeJsonStr]] =
    for
      createdAt <- OptionT(userApi.createdAtById(userId))
      history <- OptionT(historyApi.get(userId))
    yield lila.common.String.html.safeJsonValue:
      Json.toJson:
        RatingChartApi.perfTypes.map: pt =>
          Json.obj("name" -> pt.key, "points" -> ratingsMapToJson(createdAt, history(pt)))
  .value

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
