package lila
package game

import chess.Status

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports.DBObject

final class PaginatorBuilder(
    gameRepo: GameRepo,
    maxPerPage: Int) {

  def recent(page: Int): Paginator[DbGame] = 
    paginator(recentAdapter, page)

  def checkmate(page: Int): Paginator[DbGame] = 
    paginator(checkmateAdapter, page)

  private val recentAdapter = 
    adapter(DBObject())

  private val checkmateAdapter = 
    adapter(DBObject("status" -> Status.Mate.id))

  private def adapter(query: DBObject) = SalatAdapter(
    dao = gameRepo,
    query = query,
    sort = DBObject("updatedAt" -> -1)
  ) map (_.decode.get) // unsafe

  private def paginator(adapter: Adapter[DbGame], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ recent(0), identity)
}
