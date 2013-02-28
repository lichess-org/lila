package lila.app
package user

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports.DBObject

import mongodb.CachedAdapter

final class PaginatorBuilder(
    userRepo: UserRepo,
    countUsers: () ⇒ Int,
    maxPerPage: Int) {

  def elo(page: Int): Paginator[User] =
    paginator(recentAdapter, page)

  private val recentAdapter =
    adapter(DBObject("enabled" -> true))

  private def adapter(query: DBObject) = new CachedAdapter(
    dao = userRepo,
    query = query,
    sort = DBObject("elo" -> -1),
    nbResults = countUsers())

  private def paginator(adapter: Adapter[User], page: Int) = Paginator(
    adapter,
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ elo(0), identity)
}
