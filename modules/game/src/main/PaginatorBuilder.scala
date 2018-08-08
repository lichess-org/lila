package lidraughts.game

import lidraughts.common.paginator._
import lidraughts.common.MaxPerPage
import lidraughts.db.dsl._
import lidraughts.db.paginator._

private[game] final class PaginatorBuilder(
    coll: Coll,
    cached: Cached,
    maxPerPage: MaxPerPage
) {

  private val readPreference = reactivemongo.api.ReadPreference.secondaryPreferred

  import BSONHandlers.gameBSONHandler

  def recentlyCreated(selector: Bdoc, nb: Option[Int] = None) =
    apply(selector, Query.sortCreated, nb) _

  def apply(selector: Bdoc, sort: Bdoc, nb: Option[Int] = None)(page: Int): Fu[Paginator[Game]] =
    apply(nb.fold(noCacheAdapter(selector, sort)) { cached =>
      cacheAdapter(selector, sort, fuccess(cached))
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private def cacheAdapter(selector: Bdoc, sort: Bdoc, nbResults: Fu[Int]): AdapterLike[Game] =
    new CachedAdapter(
      adapter = noCacheAdapter(selector, sort),
      nbResults = nbResults
    )

  private def noCacheAdapter(selector: Bdoc, sort: Bdoc): AdapterLike[Game] =
    new Adapter[Game](
      collection = coll,
      selector = selector,
      projection = $empty,
      sort = sort,
      readPreference = readPreference
    )

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = maxPerPage)
}
