package lila.opening

import chess.opening.FullOpeningDB
import play.api.mvc.RequestHeader
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
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
)(implicit ec: ExecutionContext) {

  private val defaultCache = cacheApi.notLoading[String, Option[OpeningPage]](1024, "opening.defaultCache") {
    _.maximumSize(4096).expireAfterWrite(5 minute).buildAsync()
  }

  def index(implicit req: RequestHeader): Fu[Option[OpeningPage]] = lookup("", withWikiRevisions = false)

  def lookup(q: String, withWikiRevisions: Boolean)(implicit req: RequestHeader): Fu[Option[OpeningPage]] = {
    val config = readConfig
    if (config.isDefault && !withWikiRevisions)
      defaultCache.getFuture(q, _ => lookup(q, config, withWikiRevisions))
    else lookup(q, config, withWikiRevisions)
  }

  private def lookup(q: String, config: OpeningConfig, withWikiRevisions: Boolean): Fu[Option[OpeningPage]] =
    OpeningQuery(q, config) ?? { compute(_, withWikiRevisions) }

  private def compute(query: OpeningQuery, withWikiRevisions: Boolean): Fu[Option[OpeningPage]] =
    explorer.stats(query) zip
      explorer.queryHistory(query) zip
      allGamesHistory.get(query.config) zip
      query.openingAndExtraMoves._1.??(op => wikiApi(op, withWikiRevisions) dmap some) flatMap {
        case (((stats, history), allHistory), wiki) =>
          for {
            games <- gameRepo.gamesFromSecondary(stats.??(_.games).map(_.id))
            withPgn <- games.map { g =>
              pgnDump(g, None, PgnDump.WithFlags(evals = false)) dmap { GameWithPgn(g, _) }
            }.sequenceFu
          } yield OpeningPage(query, stats, withPgn, historyPercent(history, allHistory), wiki).some
        case _ => fuccess(none)
      }

  def readConfig(implicit req: RequestHeader) = configStore.read

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
}
