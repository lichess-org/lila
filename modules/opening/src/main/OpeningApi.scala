package lila.opening

import chess.opening.OpeningDb
import play.api.mvc.RequestHeader

import lila.db.dsl.*
import lila.game.{ GameRepo, PgnDump }
import lila.memo.CacheApi
import lila.user.User

final class OpeningApi(
    wikiApi: OpeningWikiApi,
    cacheApi: CacheApi,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    explorer: OpeningExplorer,
    configStore: OpeningConfigStore
)(using Executor):

  import OpeningQuery.Query

  private val defaultCache = cacheApi.notLoading[Query, Option[OpeningPage]](1024, "opening.defaultCache") {
    _.maximumSize(4096).expireAfterWrite(5 minute).buildAsync()
  }

  def index(using req: RequestHeader): Fu[Option[OpeningPage]] =
    lookup(Query("", none), withWikiRevisions = false, crawler = Crawler.No)

  def lookup(q: Query, withWikiRevisions: Boolean, crawler: Crawler)(using
      RequestHeader
  ): Fu[Option[OpeningPage]] =
    val config   = readConfig
    def doLookup = lookup(q, config, withWikiRevisions, crawler)
    if crawler.no && config.isDefault && !withWikiRevisions
    then defaultCache.getFuture(q, _ => doLookup)
    else doLookup

  private def lookup(
      q: Query,
      config: OpeningConfig,
      withWikiRevisions: Boolean,
      crawler: Crawler
  ): Fu[Option[OpeningPage]] =
    OpeningQuery(q, config) ?? { compute(_, withWikiRevisions, crawler) }

  private def compute(
      query: OpeningQuery,
      withWikiRevisions: Boolean,
      crawler: Crawler
  ): Fu[Option[OpeningPage]] =
    explorer.stats(query) zip
      (crawler.no ?? explorer.queryHistory(query)) zip
      allGamesHistory.get(query.config) zip
      query.openingAndExtraMoves._1.??(op => wikiApi(op, withWikiRevisions) dmap some) flatMap {
        case (((stats, history), allHistory), wiki) =>
          for {
            games <- gameRepo.gamesFromSecondary(stats.??(_.games).map(_.id))
            withPgn <- games.map { g =>
              pgnDump(g, None, PgnDump.WithFlags(evals = false)) dmap { GameWithPgn(g, _) }
            }.parallel
          } yield OpeningPage(query, stats, withPgn, historyPercent(history, allHistory), wiki).some
      }

  def readConfig(using RequestHeader) = configStore.read

  private def historyPercent(
      query: PopularityHistoryAbsolute,
      config: PopularityHistoryAbsolute
  ): PopularityHistoryPercent =
    query.zipAll(config, 0, 0) map {
      case (_, 0)     => 0
      case (cur, all) => (cur * 100f) / all
    }

  private val allGamesHistory =
    cacheApi[OpeningConfig, PopularityHistoryAbsolute](32, "opening.allGamesHistory") {
      _.expireAfterWrite(1 hour).buildAsyncFuture(explorer.configHistory)
    }
