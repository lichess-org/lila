package lila
package user

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports.DBObject

final class PaginatorBuilder(
    userRepo: UserRepo,
    countUsers: () ⇒ Int,
    maxPerPage: Int) {

  def elo(page: Int): Paginator[User] =
    paginator(recentAdapter, page)

  private val recentAdapter =
    adapter(DBObject("enabled" -> true))

  private def adapter(query: DBObject) = new Adapter[User] {
    private val salatAdapter = SalatAdapter(userRepo, query, DBObject("elo" -> -1))
    def nbResults = countUsers()
    def slice(offset: Int, length: Int) = salatAdapter.slice(offset, length)
  }

  private def paginator(adapter: Adapter[User], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ elo(0), identity)
}
