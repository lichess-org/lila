package lila
package game

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports.DBObject

final class PaginatorBuilder(
    gameRepo: GameRepo,
    maxPerPage: Int) {

  val recentAdapter = SalatAdapter(
    dao = gameRepo,
    query = DBObject(),
    sort = DBObject("updatedAt" -> -1)
  ) map { raw ⇒
      gameRepo.decode(raw).get // unsafe
    }

  def recent(page: Int): Paginator[DbGame] = Paginator(
    recentAdapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ recent(0), identity)
}
