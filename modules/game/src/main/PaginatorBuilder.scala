package lila.game

import play.api.libs.json._

import chess.Status
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Types.Sort
import tube.gameTube

private[game] final class PaginatorBuilder(cached: Cached, maxPerPage: Int) {

  private val readPreference = reactivemongo.api.ReadPreference.secondaryPreferred

  def recentlyCreated(selector: JsObject, nb: Option[Int] = None) =
    apply(selector, Seq(Query.sortCreated), nb) _

  def apply(selector: JsObject, sort: Sort, nb: Option[Int] = None)(page: Int): Fu[Paginator[Game]] =
    apply(nb.fold(noCacheAdapter(selector, sort)) { cached =>
      cacheAdapter(selector, sort, fuccess(cached))
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private def cacheAdapter(selector: JsObject, sort: Sort, nbResults: Fu[Int]): AdapterLike[Game] =
    new CachedAdapter(
      adapter = noCacheAdapter(selector, sort),
      nbResults = nbResults)

  private def noCacheAdapter(selector: JsObject, sort: Sort): AdapterLike[Game] =
    new Adapter(
      selector = selector,
      sort = sort,
      readPreference = readPreference)

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = maxPerPage)
}
