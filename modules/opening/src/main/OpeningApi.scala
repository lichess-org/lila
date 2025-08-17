package lila.opening

import play.api.mvc.RequestHeader

import lila.core.game.{ GameRepo, PgnDump }
import lila.core.net.Crawler
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

  def index(using RequestHeader, OpeningAccessControl): Fu[Option[OpeningPage]] =
    lookup(Query("", none), withWikiRevisions = false, crawler = Crawler.No)

  def lookup(q: Query, withWikiRevisions: Boolean, crawler: Crawler)(using
      RequestHeader,
      OpeningAccessControl
  ): Fu[Option[OpeningPage]] =
    val config = if crawler.yes then OpeningConfig.default else readConfig
    def doLookup = lookup(q, config, withWikiRevisions, crawler)
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
      crawler: Crawler
  )(using OpeningAccessControl): Fu[Option[OpeningPage]] =
    OpeningQuery(q, config).so { compute(_, withWikiRevisions, crawler) }

  private def compute(
      query: OpeningQuery,
      withWikiRevisions: Boolean,
      crawler: Crawler
  )(using accessControl: OpeningAccessControl): Fu[Option[OpeningPage]] =
    for
      wiki <- query.closestOpening.traverse(wikiApi(_, withWikiRevisions))
      loadStats <- accessControl.canLoadExpensiveStats(wiki.exists(_.hasMarkup), crawler)
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
    cacheApi[OpeningConfig, PopularityHistoryAbsolute](32, "opening.allGamesHistory"):
      _.expireAfterWrite(1.hour).buildAsyncFuture: config =>
        explorer
          .stats(Vector.empty, config, Crawler(false))
          .map(_.toOption.flatten.so(_.popularityHistory))
