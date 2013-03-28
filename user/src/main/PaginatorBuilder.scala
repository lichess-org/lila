package lila.user

import lila.common.paginator._
import lila.db.paginator._

import play.api.libs.json._

final class PaginatorBuilder(
    countUsers: Fu[Int],
    maxPerPage: Int) {

  import UserRepo.tube

  def elo(page: Int): Fu[Paginator[User]] = paginator(recentAdapter, page)

  private val recentAdapter: AdapterLike[User] = adapter(Json.obj("enabled" -> true))

  private def adapter(selector: JsObject): AdapterLike[User] = new CachedAdapter(
    adapter = new Adapter(
      selector = selector,
      sort = Seq(UserRepo.sortEloDesc)
    ),
    nbResults = countUsers
  )

  private def paginator(adapter: AdapterLike[User], page: Int): Fu[Paginator[User]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )
}
