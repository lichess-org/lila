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
    explorer.stats(query) zip explorer.history(query) map {
      case (Some(stats), history) => OpeningPage(query, stats, history).some
      case _                      => none
    }
}
