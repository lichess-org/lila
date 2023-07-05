package lila.game

import lila.common.paginator.*
import lila.common.config.MaxPerPage
import lila.db.dsl.*
import lila.db.paginator.*

final class PaginatorBuilder(gameRepo: GameRepo)(using Executor):

  import BSONHandlers.gameBSONHandler

  def recentlyCreated(selector: Bdoc, nb: Option[Int] = None) =
    apply(selector, Query.sortCreated, nb)

  def apply(selector: Bdoc, sort: Bdoc, nb: Option[Int] = None)(page: Int): Fu[Paginator[Game]] =
    apply(nb.fold[AdapterLike[Game]](noCacheAdapter(selector, sort)) { cached =>
      noCacheAdapter(selector, sort) withNbResults fuccess(cached)
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private def noCacheAdapter(selector: Bdoc, sort: Bdoc) = Adapter[Game](
    collection = gameRepo.coll,
    selector = selector,
    projection = none,
    sort = sort,
    _.sec
  )

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = MaxPerPage(12))
