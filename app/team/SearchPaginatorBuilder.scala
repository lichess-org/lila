package lila
package team

import game.DbGame

import com.github.ornicar.paginator._

private[team] final class SearchPaginatorBuilder(
    indexer: SearchIndexer,
    maxPerPage: Int) {

  def apply(query: SearchQuery, page: Int): Paginator[Team] = Paginator(
    adapter = new ESAdapter(indexer, query),
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ apply(query, 0), identity)

  private class ESAdapter(
      indexer: SearchIndexer,
      query: SearchQuery) extends Adapter[Team] {

    def nbResults = indexer count query.countRequest

    def slice(offset: Int, length: Int) = indexer toTeams {
      indexer search query.searchRequest(offset, length)
    } unsafePerformIO
  }
}
