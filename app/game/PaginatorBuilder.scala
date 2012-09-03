package lila
package game

import user.User
import chess.Status
import mongodb.CachedAdapter

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._

final class PaginatorBuilder(
    gameRepo: GameRepo,
    cached: Cached,
    maxPerPage: Int) {

  def recent(page: Int): Paginator[DbGame] =
    paginator(recentAdapter, page)

  def checkmate(page: Int): Paginator[DbGame] =
    paginator(checkmateAdapter, page)

  def popular(page: Int): Paginator[DbGame] =
    paginator(popularAdapter, page)

  def recentlyCreated(query: DBObject, nb: Option[Int] = None) = 
    apply(query, Query.sortCreated, nb) _

  def apply(query: DBObject, sort: DBObject, nb: Option[Int] = None)(page: Int): Paginator[DbGame] =
    apply(nb.fold(
      cached ⇒ adapter(query, sort, cached),
      noCacheAdapter(query, sort)
    ))(page)

  private def apply(adapter: Adapter[DbGame])(page: Int): Paginator[DbGame] =
    paginator(adapter, page)

  private val recentAdapter =
    adapter(DBObject(), Query.sortCreated, cached.nbGames)

  private val checkmateAdapter =
    adapter(Query.mate, Query.sortCreated, cached.nbMates)

  private val popularAdapter =
    adapter(Query.popular, Query.sortPopular, cached.nbPopular)

  private def adapter(
    query: DBObject,
    sort: DBObject,
    nbResults: Int) = new CachedAdapter(
    dao = gameRepo,
    query = query,
    sort = sort,
    nbResults = nbResults
  ) map (_.decode.get) // unsafe

  private def noCacheAdapter(query: DBObject, sort: DBObject) = SalatAdapter(
    dao = gameRepo,
    query = query,
    sort = sort
  ) map (_.decode.get) // unsafe

  private def paginator(adapter: Adapter[DbGame], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ recent(0), identity)
}
