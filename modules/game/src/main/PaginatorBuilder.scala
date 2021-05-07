package lila.game

import reactivemongo.api.ReadPreference

import lila.common.paginator._
import lila.common.config.MaxPerPage
import lila.db.dsl._
import lila.db.paginator._

final class PaginatorBuilder(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

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
      collection = gameRepo.coll,
      selector = selector,
      projection = none,
      sort = sort,
      readPreference = ReadPreference.secondaryPreferred
    )

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = MaxPerPage(12))
}
