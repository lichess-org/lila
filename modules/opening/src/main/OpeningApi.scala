package lila.opening

import chess.opening.FullOpeningDB
import play.api.mvc.RequestHeader
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi

final class OpeningApi(
    cacheApi: CacheApi,
    explorer: OpeningExplorer,
    configStore: OpeningConfigStore
)(implicit ec: ExecutionContext) {

  def index(implicit req: RequestHeader): Fu[Option[OpeningPage]] = lookup("")

  def lookup(q: String)(implicit req: RequestHeader): Fu[Option[OpeningPage]] =
    OpeningQuery(q, configStore.read) ?? lookup

  def lookup(query: OpeningQuery): Fu[Option[OpeningPage]] =
    explorer.stats(query) zip explorer.queryHistory(query) zip allGamesHistory.get(query.config) map {
      case ((Some(stats), history), allHistory) =>
        OpeningPage(query, stats, ponderHistory(history, allHistory)).some
      case _ => none
    }

  private def ponderHistory(query: PopularityHistory, config: PopularityHistory): PopularityHistory =
    query.zipAll(config, 0, 0) map {
      case (_, 0)     => 0
      case (cur, all) => ((cur * 10_000L) / all).toInt
    }

  private val allGamesHistory = cacheApi[OpeningConfig, PopularityHistory](32, "opening.allGamesHistory") {
    _.expireAfterWrite(1 hour).buildAsyncFuture(explorer.configHistory)
  }
}
