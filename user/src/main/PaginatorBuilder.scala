package lila.user

import lila.common.paginator._
import lila.db.paginator._

import play.api.libs.json._

final class PaginatorBuilder(
  userRepo: UserRepo, 
  countUsers: Fu[Int], 
  maxPerPage: Int) {

  def elo(page: Int): Fu[Paginator[User]] = paginator(recentAdapter, page)

  private implicit val coll = userRepo.coll

  private val recentAdapter: AdapterLike[User] = adapter(Json.obj("enabled" -> true))

  private def adapter(selector: JsObject): AdapterLike[User] = new CachedAdapter(
    adapter = new Adapter(
      selector = selector,
      sort = Seq(userRepo.sortEloDesc)
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
