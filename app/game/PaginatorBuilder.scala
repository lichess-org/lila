package lila
package game

import user.User
import chess.Status

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._

final class PaginatorBuilder(
    gameRepo: GameRepo,
    maxPerPage: Int) {

  def recent(page: Int): Paginator[DbGame] = 
    paginator(recentAdapter, page)

  def checkmate(page: Int): Paginator[DbGame] = 
    paginator(checkmateAdapter, page)

  def popular(page: Int): Paginator[DbGame] = 
    paginator(popularAdapter, page)

  def recentlyUpdated(query: DBObject) = apply(query, Query.sortUpdated) _

  def recentlyCreated(query: DBObject) = apply(query, Query.sortCreated) _

  def apply(query: DBObject, sort: DBObject)(page: Int): Paginator[DbGame] = 
    apply(adapter(query, sort))(page)

  private def apply(adapter: Adapter[DbGame])(page: Int): Paginator[DbGame] =
    paginator(adapter, page)

  private val recentAdapter = 
    adapter(DBObject(), Query.sortUpdated)

  private val checkmateAdapter = 
    adapter(Query.mate, Query.sortUpdated)

  private val popularAdapter = 
    adapter(Query.popular, Query.sortPopular)

  private def adapter(query: DBObject, sort: DBObject) = SalatAdapter(
    dao = gameRepo,
    query = query,
    sort = sort
  ) map (_.decode.get) // unsafe

  private def paginator(adapter: Adapter[DbGame], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ recent(0), identity)
}
