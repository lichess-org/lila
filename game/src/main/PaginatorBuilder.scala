package lila.game

import chess.Status

import lila.common.paginator._
import lila.db.paginator._
import lila.db.Types.Sort

import play.api.libs.json._

private[game] final class PaginatorBuilder(
    gameRepo: GameRepo,
    cached: Cached,
    maxPerPage: Int) {

  def recent(page: Int): Fu[Paginator[Game]] =
    paginator(recentAdapter, page)

  def checkmate(page: Int): Fu[Paginator[Game]] =
    paginator(checkmateAdapter, page)

  def popular(page: Int): Fu[Paginator[Game]] =
    paginator(popularAdapter, page)

  def imported(page: Int): Fu[Paginator[Game]] =
    paginator(importedAdapter, page)

  def recentlyCreated(query: JsObject, nb: Option[Fu[Int]] = None) = 
    apply(query, Seq(Query.sortCreated), nb) _

  def apply(query: JsObject, sort: Sort, nb: Option[Fu[Int]] = None)(page: Int): Fu[Paginator[Game]] =
    apply(nb.fold(noCacheAdapter(query, sort)) { cached ⇒
      cacheAdapter(query, sort, cached)
    })(page)

  private def apply(adapter: AdapterLike[Game])(page: Int): Fu[Paginator[Game]] =
    paginator(adapter, page)

  private val recentAdapter =
    cacheAdapter(Query.all, Seq(Query.sortCreated), cached.nbGames)

  private val checkmateAdapter =
    cacheAdapter(Query.mate, Seq(Query.sortCreated), cached.nbMates)

  private val popularAdapter =
    cacheAdapter(Query.popular, Seq(Query.sortPopular), cached.nbPopular)

  private def importedAdapter =
    cacheAdapter(Query.imported, Seq(Query.sortCreated), cached.nbImported)

  private def cacheAdapter(query: JsObject, sort: Sort, nbResults: Fu[Int]): AdapterLike[Game] =
    new CachedAdapter(
      adapter = noCacheAdapter(query, sort),
      nbResults = nbResults)

  private def noCacheAdapter(query: JsObject, sort: Sort): AdapterLike[Game] =
    new Adapter(repo = gameRepo, query = query, sort = sort)

  private def paginator(adapter: AdapterLike[Game], page: Int): Fu[Paginator[Game]] =
    Paginator(adapter, currentPage = page, maxPerPage = maxPerPage)
}
