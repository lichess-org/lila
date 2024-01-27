package lila.user

import reactivemongo.api.bson.*

import lila.common.LightUser
import lila.memo.CacheApi.*
import lila.rating.{ Perf, PerfType }
import lila.db.dsl.{ given, * }
import User.{ LightCount, LightPerf }

final class Cached(
    userRepo: UserRepo,
    userApi: UserApi,
    onlineUserIds: lila.socket.OnlineIds,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    rankingApi: RankingApi
)(using Executor, Scheduler):

  private given BSONDocumentHandler[LightUser]  = Macros.handler
  private given BSONDocumentHandler[LightPerf]  = Macros.handler
  private given BSONDocumentHandler[LightCount] = Macros.handler

  val top10 = cacheApi.unit[UserPerfs.Leaderboards]:
    _.refreshAfterWrite(2 minutes).buildAsyncFuture: _ =>
      rankingApi
        .fetchLeaderboard(10)
        .withTimeout(2 minutes, "user.Cached.top10")
        .monSuccess(_.user.leaderboardCompute)

  val top200Perf = mongoCache[Perf.Id, List[User.LightPerf]](
    PerfType.leaderboardable.size,
    "user:top200:perf",
    19 minutes,
    _.toString
  ): loader =>
    _.refreshAfterWrite(20 minutes).buildAsyncFuture:
      loader:
        rankingApi.topPerf(_, 200)

  private val topWeekCache = mongoCache.unit[List[User.LightPerf]](
    "user:top:week",
    9 minutes
  ): loader =>
    _.refreshAfterWrite(10 minutes).buildAsyncFuture:
      loader: _ =>
        PerfType.leaderboardable
          .map { perf =>
            rankingApi.topPerf(perf.id, 1)
          }
          .parallel
          .dmap(_.flatten)

  def topWeek = topWeekCache.get {}

  val top10NbGame = mongoCache.unit[List[User.LightCount]](
    "user:top:nbGame",
    74 minutes
  ): loader =>
    _.refreshAfterWrite(75 minutes).buildAsyncFuture:
      loader: _ =>
        userRepo topNbGame 10 dmap (_.map(_.lightCount))

  private val top50OnlineCache = cacheApi.unit[List[User.WithPerfs]]:
    _.refreshAfterWrite(1 minute).buildAsyncFuture: _ =>
      userApi.byIdsSortRatingNoBot(onlineUserIds(), 50)

  def getTop50Online = top50OnlineCache.getUnit

  def rankingsOf(userId: UserId): lila.rating.UserRankMap = rankingApi.weeklyStableRanking of userId

  private[user] val botIds = cacheApi.unit[Set[UserId]]:
    _.refreshAfterWrite(5 minutes).buildAsyncFuture(_ => userRepo.botIds)

  private def userIdsLikeFetch(text: UserSearch) =
    userRepo.userIdsLikeFilter(text, $empty, 12)

  private val userIdsLikeCache = cacheApi[UserSearch, List[UserId]](1024, "user.like"):
    _.expireAfterWrite(5 minutes).buildAsyncFuture(userIdsLikeFetch)

  def userIdsLike(text: UserSearch): Fu[List[UserId]] =
    if text.value.lengthIs < 5 then userIdsLikeCache get text
    else userIdsLikeFetch(text)
