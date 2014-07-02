package lila.game

import play.api.libs.json._

import chess.Status
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Types.Sort
import tube.gameTube

private[game] final class PaginatorBuilder(cached: Cached, maxPerPage: Int) {

  def recent(page: Int): Fu[Paginator[Game]] =
    paginator(recentAdapter, page)

  def checkmate(page: Int): Fu[Paginator[Game]] =
    paginator(checkmateAdapter, page)

  def imported(page: Int): Fu[Paginator[Game]] =
    paginator(importedAdapter, page)

  def recentlyCreated(selector: JsObject, nb: Option[Int] = None) =
    apply(selector, Seq(Query.sortCreated), nb) _

  def apply(selector: JsObject, sort: Sort, nb: Option[Int] = None)(page: Int): Fu[Paginator[Game]] =
    apply(nb.fold(noCacheAdapter(selector, sort)) { cached =>
      cacheAdapter(selector, sort, fuccess(cached))
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private val recentAdapter =
    cacheAdapter(Query.all, Seq(Query.sortCreated), cached.nbGames)

  private val checkmateAdapter =
    cacheAdapter(Query.mate, Seq(Query.sortCreated), cached.nbMates)

  private def importedAdapter =
    cacheAdapter(Query.imported, Seq(Query.sortCreated), cached.nbImported)

  private def cacheAdapter(selector: JsObject, sort: Sort, nbResults: Fu[Int]): AdapterLike[Game] =
    new CachedAdapter(
      adapter = noCacheAdapter(selector, sort),
      nbResults = nbResults)

  private def noCacheAdapter(selector: JsObject, sort: Sort): AdapterLike[Game] =
    new Adapter(selector = selector, sort = sort)

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = maxPerPage)
}
