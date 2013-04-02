package lila.user

import lila.common.paginator._
import lila.db.paginator._

import play.api.libs.json._

final class PaginatorBuilder(
    countUsers: Fu[Int],
    maxPerPage: Int) {

  private implicit def userT = userTube

  def elo(page: Int): Fu[Paginator[User]] = Paginator(
    adapter = recentAdapter,
    currentPage = page,
    maxPerPage = maxPerPage)

  private val recentAdapter: AdapterLike[User] = adapter(Json.obj("enabled" -> true))

  private def adapter(selector: JsObject): AdapterLike[User] = new CachedAdapter(
    adapter = new Adapter(
      selector = selector,
      sort = Seq(UserRepo.sortEloDesc)
    ),
    nbResults = countUsers
  )
}
