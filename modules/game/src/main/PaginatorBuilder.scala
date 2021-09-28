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
    apply(nb.fold[AdapterLike[Game]](noCacheAdapter(selector, sort)) { cached =>
      noCacheAdapter(selector, sort) withNbResults fuccess(cached)
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private def noCacheAdapter(selector: Bdoc, sort: Bdoc) =
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
