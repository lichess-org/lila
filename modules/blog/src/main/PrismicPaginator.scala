package lidraughts.blog

import io.prismic._

import lidraughts.common.MaxPerPage
import lidraughts.common.paginator._

private object PrismicPaginator {

  def apply(response: Response, page: Int, maxPerPage: MaxPerPage): Paginator[Document] =
    Paginator.fromResults(
      currentPageResults = response.results,
      nbResults = response.totalResultsSize,
      currentPage = page,
      maxPerPage = maxPerPage
    )
}