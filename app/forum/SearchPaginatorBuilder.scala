package lila
package forum

import com.github.ornicar.paginator._

private[forum] final class SearchPaginatorBuilder(
    indexer: SearchIndexer,
    maxPerPage: Int) {

  def apply(text: String, page: Int): Paginator[PostView] = Paginator(
    adapter = new ESAdapter(indexer, SearchQuery(text)),
    currentPage = page,
    maxPerPage = maxPerPage).fold(_ ⇒ apply(text, 0), identity)

  private class ESAdapter(indexer: SearchIndexer, query: SearchQuery) extends Adapter[PostView] {

    def nbResults = indexer count query.countRequest

    def slice(offset: Int, length: Int) = indexer toViews {
      indexer search query.searchRequest(offset, length)
    } unsafePerformIO
  }
}
