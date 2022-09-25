package lila.opening

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.db.dsl._
import lila.memo.CacheApi
import chess.opening.FullOpening
import chess.opening.FullOpeningDB

final class OpeningApi(
    cacheApi: CacheApi,
    explorer: OpeningExplorer
)(implicit ec: ExecutionContext) {

  def lookup(q: String): Fu[Option[OpeningPage]] = {
    OpeningQuery.justFen(q) orElse OpeningQuery.byOpening(q)
  } ?? lookup

  def lookup(query: OpeningQuery): Fu[Option[OpeningPage]] =
    explorer(query) map {
      _ map { explored =>
        OpeningPage(query, explored)
      }
    }
}
