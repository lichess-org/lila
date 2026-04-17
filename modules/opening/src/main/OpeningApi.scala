package lila.opening

import play.api.mvc.RequestHeader

import lila.core.game.{ GameRepo, PgnDump }
import lila.core.net.Crawler
import lila.core.security.IsProxy
import lila.memo.{ CacheApi, RateLimit }

final class OpeningApi(
    wikiApi: OpeningWikiApi,
    cacheApi: CacheApi,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    explorer: OpeningExplorer,
    configStore: OpeningConfigStore
)(using Executor, lila.core.config.RateLimit):

  import OpeningQuery.Query

  private val defaultCache = cacheApi.notLoadingSync[Query, OpeningPage](1024, "opening.defaultCache"):
    _.maximumSize(4096).expireAfterWrite(10.minutes).build()

  private val userRateLimit = RateLimit[UserId](30, 2.minutes, "opening.stats.user")

  def index(using RequestHeader, Option[Me]): Fu[Option[OpeningPage]] =
    lookup(Query("", none), crawler = Crawler.No, proxy = IsProxy.empty)

  def lookup(q: Query, crawler: Crawler, proxy: IsProxy)(using
      RequestHeader,
      Option[Me]
  ): Fu[Option[OpeningPage]] =
    val config = if crawler.yes then OpeningConfig.default else readConfig
    def doLookup = lookup(q, config, crawler, proxy)
    if crawler.no && config.isDefault
    then
      defaultCache.getIfPresent(q) match
        case Some(page) => fuccess(page.some)
        case None =>
          doLookup.addEffect:
            _.filter(_.explored.isSuccess).foreach:
              defaultCache.put(q, _)
    else doLookup

  private def lookup(
      q: Query,
      config: OpeningConfig,
      crawler: Crawler,
      proxy: IsProxy
  )(using Option[Me]): Fu[Option[OpeningPage]] =
    OpeningQuery(q, config).so { compute(_, crawler, proxy) }

  private def compute(
      query: OpeningQuery,
      crawler: Crawler,
      proxy: IsProxy
  )(using me: Option[Me]): Fu[Option[OpeningPage]] =
    for
      wiki <- query.closestOpening.traverse(wikiApi.apply)
      loadStats = query.sans.size < 4 ||
        me.map(_.userId).exists(userRateLimit.hit(_, if proxy.yes then 3 else 1))
      stats <-
        if loadStats then explorer.stats(query.uci, query.config, crawler)
        else
          val error =
            if me.isDefined then "Please wait a bit before loading more opening pages."
            else "Please log in to load more opening pages."
          fuccess(scala.util.Failure(WaitOrLogin(error)))
      statsOption = stats.toOption.flatten
      allHistory <- allGamesHistory.get(query.config)
      games <- gameRepo.gamesFromSecondary(statsOption.so(_.games).map(_.id))
      withPgn <- games.traverse: g =>
        pgnDump(g, None, PgnDump.WithFlags(evals = false)).dmap { GameWithPgn(g, _) }
      history = statsOption.so(_.popularityHistory)
      relHistory = query.uci.nonEmpty.so(historyPercent(history, allHistory))
    yield makeOpeningPage(query, stats, withPgn, relHistory, wiki).some

  def readConfig(using RequestHeader) = configStore.read

  private def historyPercent(
      query: PopularityHistoryAbsolute,
      config: PopularityHistoryAbsolute
  ): PopularityHistoryPercent =
    query
      .zipAll(config, 0L, 0L)
      .map:
        case (_, 0) => 0
        case (cur, all) => ((cur.toDouble / all) * 100).toFloat

  private val allGamesHistory =
    cacheApi[OpeningConfig, PopularityHistoryAbsolute](64, "opening.allGamesHistory"):
      _.expireAfterWrite(1.hour).buildAsyncFuture: config =>
        explorer
          .stats(Vector.empty, config, Crawler(false))
          .map(_.toOption.flatten.so(_.popularityHistory))
