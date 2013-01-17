package lila
package search

import game.DbGame

import com.github.ornicar.paginator._

private[search] final class PaginatorBuilder(
    indexer: GameIndexer,
    maxPerPage: Int) {

  def apply(query: Query, page: Int): Paginator[DbGame] = Paginator(
    adapter = new ESAdapter(indexer, query),
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ apply(query, 0), identity)
}

private[search] final class ESAdapter(
    indexer: GameIndexer,
    query: Query) extends Adapter[DbGame] {

  def nbResults = indexer count query.countRequest

  def slice(offset: Int, length: Int) = indexer toGames {
    indexer search query.searchRequest(offset, length)
  } unsafePerformIO

}
