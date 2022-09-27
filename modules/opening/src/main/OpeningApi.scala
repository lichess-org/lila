package lila.opening

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaOpeningFamily, SimpleOpening }
import lila.db.dsl._
import lila.memo.CacheApi
import chess.opening.FullOpeningDB

final class OpeningApi(
    cacheApi: CacheApi,
    explorer: OpeningExplorer
)(implicit ec: ExecutionContext) {

  def index: Fu[Option[OpeningPage]] = lookup("")

  def lookup(q: String): Fu[Option[OpeningPage]] = OpeningQuery(q) ?? lookup

  def lookup(query: OpeningQuery): Fu[Option[OpeningPage]] =
    explorer(query) map {
      _ map { explored =>
        OpeningPage(query, explored)
      }
    }
}
