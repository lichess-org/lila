package lila
package user

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports.DBObject

final class PaginatorBuilder(
    userRepo: UserRepo,
    maxPerPage: Int) {

  def elo(page: Int): Paginator[User] = 
    paginator(recentAdapter, page)

  private val recentAdapter = 
    adapter(DBObject("enabled" -> true))

  private def adapter(query: DBObject) = SalatAdapter(
    dao = userRepo,
    query = query,
    sort = DBObject("elo" -> -1)
  ) 

  private def paginator(adapter: Adapter[User], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ elo(0), identity)
}
