package lila
package search

import game.DbGame

import com.github.ornicar.paginator._

final class PaginatorBuilder(
    indexer: Indexer,
    maxPerPage: Int) {

  def apply(query: Query, page: Int): Paginator[DbGame] = Paginator(
    adapter = new ESAdapter(indexer, query),
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ apply(query, 0), identity)
}

final class ESAdapter(
    indexer: Indexer,
    query: Query) extends Adapter[DbGame] {

  def nbResults = indexer count query.countRequest

  def slice(offset: Int, length: Int) = indexer toGames {
    indexer search query.searchRequest(offset, length)
  } unsafePerformIO

}
