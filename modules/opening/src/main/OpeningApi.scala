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

  private def ponderHistory(query: OpeningHistory[Int], config: OpeningHistory[Int]): OpeningHistory[Int] =
    query zip config map { case (cur, all) =>
      if (all.sum < 1) cur.copy(black = 0, draws = 0, white = 0)
      else
        cur.copy(
          black = ((cur.black.toFloat / all.sum) * 10_000).toInt,
          draws = ((cur.draws.toFloat / all.sum) * 10_000).toInt,
          white = ((cur.white.toFloat / all.sum) * 10_000).toInt
        )
    }

  private val allGamesHistory = cacheApi[OpeningConfig, OpeningHistory[Int]](32, "opening.allGamesHistory") {
    _.expireAfterWrite(1 hour).buildAsyncFuture(explorer.configHistory)
  }
}
