package lila.opening

import play.api.mvc.RequestHeader

import lila.core.game.{ GameRepo, PgnDump }
import lila.core.net.Crawler
import lila.core.security.IsProxy
import lila.memo.CacheApi

final class OpeningApi(
    wikiApi: OpeningWikiApi,
    cacheApi: CacheApi,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    explorer: OpeningExplorer,
    configStore: OpeningConfigStore
)(using Executor):

  import OpeningQuery.Query

  private val defaultCache = cacheApi.notLoading[Query, Option[OpeningPage]](1024, "opening.defaultCache"):
    _.maximumSize(4096).expireAfterWrite(10.minutes).buildAsync()

  def index(using RequestHeader, Option[MyId]): Fu[Option[OpeningPage]] =
    lookup(Query("", none), withWikiRevisions = false, crawler = Crawler.No, proxy = IsProxy.empty)

  def lookup(q: Query, withWikiRevisions: Boolean, crawler: Crawler, proxy: IsProxy)(using
      RequestHeader,
      Option[MyId]
  ): Fu[Option[OpeningPage]] =
    val config = if crawler.yes then OpeningConfig.default else readConfig
    def doLookup = lookup(q, config, withWikiRevisions, crawler, proxy)
    if crawler.no && config.isDefault && !withWikiRevisions
    then
      defaultCache
        .getFuture(q, _ => doLookup)
        .addEffect:
          _.foreach: page =>
            if page.explored.isFailure then defaultCache.synchronous().invalidate(q)
    else doLookup

  private def lookup(
      q: Query,
      config: OpeningConfig,
      withWikiRevisions: Boolean,
      crawler: Crawler,
      proxy: IsProxy
  )(using Option[MyId]): Fu[Option[OpeningPage]] =
    OpeningQuery(q, config).so { compute(_, withWikiRevisions, crawler, proxy) }

  private def compute(
      query: OpeningQuery,
      withWikiRevisions: Boolean,
      crawler: Crawler,
      proxy: IsProxy
  )(using Option[MyId]): Fu[Option[OpeningPage]] =
    for
      wiki <- query.closestOpening.traverse(wikiApi(_, withWikiRevisions))
      loadStats = canLoadExpensiveStats(wiki.exists(_.hasMarkup), crawler, proxy)
      stats <-
        if loadStats then explorer.stats(query.uci, query.config, crawler)
        else fuccess(scala.util.Success(none))
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
          .stats(Vector.empty, config, Crawler(false))(using UserId.lichess.into(MyId).some)
          .map(_.toOption.flatten.so(_.popularityHistory))

  private def canLoadExpensiveStats(wikiMarkup: Boolean, crawler: Crawler, proxy: IsProxy)(using
      me: Option[MyId]
  ): Boolean =
    if (crawler.yes || proxy.yes) && !wikiMarkup
    then false // nothing for crawlers to index if we don't have our own text
    else
      proxy.no || // legit IPs are always allowed
      me.isDefined // only allow proxy IPs if they have a session
