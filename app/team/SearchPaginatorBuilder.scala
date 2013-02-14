package lila
package team

import com.github.ornicar.paginator._

private[team] final class SearchPaginatorBuilder(
    indexer: SearchIndexer,
    maxPerPage: Int) {

  def apply(text: String, page: Int): Paginator[Team] = Paginator(
    adapter = new ESAdapter(indexer, SearchQuery(text)),
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ apply(text, 0), identity)

  private class ESAdapter(indexer: SearchIndexer, query: SearchQuery) extends Adapter[Team] {

    def nbResults = indexer count query.countRequest

    def slice(offset: Int, length: Int) = indexer toTeams {
      indexer search query.searchRequest(offset, length)
    } unsafePerformIO
  }
}
