package lila.opening

import chess.opening.FullOpeningDB
import play.api.mvc.RequestHeader
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.game.{ GameRepo, PgnDump }
import lila.memo.CacheApi

final class OpeningApi(
    cacheApi: CacheApi,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    explorer: OpeningExplorer,
    configStore: OpeningConfigStore
)(implicit ec: ExecutionContext) {

  def index(implicit req: RequestHeader): Fu[Option[OpeningPage]] = lookup("")

  def lookup(q: String)(implicit req: RequestHeader): Fu[Option[OpeningPage]] =
    OpeningQuery(q, readConfig) ?? lookup

  def lookup(query: OpeningQuery): Fu[Option[OpeningPage]] =
    explorer.stats(query) zip explorer.queryHistory(query) zip allGamesHistory.get(query.config) flatMap {
      case ((Some(stats), history), allHistory) =>
        for {
          games <- gameRepo.gamesFromSecondary(stats.games.map(_.id))
          withPgn <- games.map { g =>
            pgnDump(g, None, PgnDump.WithFlags(evals = false)) dmap { GameWithPgn(g, _) }
          }.sequenceFu
        } yield OpeningPage(query, stats, withPgn, historyPercent(history, allHistory)).some
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
