package lila.user

import reactivemongo.api.bson.*

import lila.core.perf.UserWithPerfs
import lila.core.user.LightPerf
import lila.core.userId.UserSearch
import lila.core.rating.UserRankMap
import lila.db.dsl.*
import lila.memo.CacheApi.*
import lila.rating.{ PerfType, UserPerfs }
import scalalib.paginator.Paginator
import chess.rating.IntRatingDiff
import lila.core.LightUser
import lila.core.userId.UserName
import chess.IntRating

final class Cached(
    userRepo: UserRepo,
    userApi: UserApi,
    onlineUserIds: lila.core.socket.OnlineIds,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    rankingApi: RankingApi
)(using Executor, Scheduler)
    extends lila.core.user.CachedApi:

  import BSONHandlers.given

  // DEBUG: hardcode users onto leaderboards (dev/local only)
  private def debugInjected(perf: PerfKey): List[LightPerf] =
    if perf == PerfKey.classical then
      List(
        LightPerf(LightUser.fallback(UserName("abcdefghijklmnopqrst")), perf, IntRating(4000), IntRatingDiff(+12)),
        LightPerf(LightUser.fallback(UserName("BBBB")), perf, IntRating(3990), IntRatingDiff(-5)),
        LightPerf(LightUser.fallback(UserName("CCCC")), perf, IntRating(3980), IntRatingDiff(0))
      )
    else Nil

  private def injectLeaderboardsDebug(lbs: UserPerfs.Leaderboards): UserPerfs.Leaderboards =
    val injected = debugInjected(PerfKey.classical)
    if injected.isEmpty then lbs
    else
      val injectedIds = injected.map(_.user.id).toSet
      val merged = (injected ++ lbs.classical.filterNot(lp => injectedIds.contains(lp.user.id))).take(10)
      lbs.copy(classical = merged)


  val top10 = cacheApi.unit[UserPerfs.Leaderboards]:
    _.refreshAfterWrite(2.minutes).buildAsyncTimeout(2.minutes): _ =>
      rankingApi.fetchLeaderboard(10).map(injectLeaderboardsDebug).monSuccess(_.user.leaderboardCompute)

  private val topPerfFirstPage = mongoCache[PerfKey, Seq[LightPerf]](
    PerfType.leaderboardable.size,
    "user:top:perf:firstPage",
    10.minutes,
    _.value
  ): loader =>
    _.refreshAfterWrite(10.minutes).buildAsyncFuture:
      loader: perf =>
        rankingApi.topPerf.pager(perf, 1).map(_.currentPageResults)

  export topPerfFirstPage.get as firstPageOf

  def topPerfPager(perf: PerfKey, page: Int): Fu[Paginator[LightPerf]] =
    if page == 1 then
      for users <- firstPageOf(perf)
      yield
        val injected = debugInjected(perf)
        val injectedIds = injected.map(_.user.id).toSet
        val merged =
          (injected ++ users.filterNot(lp => injectedIds.contains(lp.user.id)))
            .take(100)
        Paginator.fromResults(
          merged,
          nbResults = 500_000,
          currentPage = page,
          rankingApi.topPerf.maxPerPage
        )
    else rankingApi.topPerf.pager(perf, page)

  val top10NbGame = mongoCache.unit[List[LightCount]](
    "user:top:nbGame",
    74.minutes
  ): loader =>
    _.refreshAfterWrite(75.minutes).buildAsyncFuture:
      loader: _ =>
        userRepo
          .topNbGame(10)
          .dmap(_.map(u => LightCount(u.light, u.count.game)))

  private val top50OnlineCache = cacheApi.unit[List[UserWithPerfs]]:
    _.refreshAfterWrite(1.minute).buildAsyncTimeout(): _ =>
      userApi.byIdsSortRatingNoBot(onlineUserIds.exec(), 50)

  def getTop50Online: Fu[List[UserWithPerfs]] = top50OnlineCache.getUnit

  def rankingsOf(userId: UserId): UserRankMap = rankingApi.weeklyStableRanking.of(userId)

  private val botIds = cacheApi.unit[Set[UserId]]:
    _.refreshAfterWrite(5.minutes).buildAsyncTimeout()(_ => userRepo.botIds)

  def getBotIds: Fu[Set[UserId]] = botIds.getUnit

  private def userIdsLikeFetch(text: UserSearch) =
    userRepo.userIdsLikeFilter(text, $empty, 12)

  private val userIdsLikeCache = cacheApi[UserSearch, List[UserId]](1024, "user.like"):
    _.expireAfterWrite(5.minutes).buildAsyncTimeout()(userIdsLikeFetch)

  def userIdsLike(text: UserSearch): Fu[List[UserId]] =
    if text.value.lengthIs < 5 then userIdsLikeCache.get(text)
    else userIdsLikeFetch(text)
