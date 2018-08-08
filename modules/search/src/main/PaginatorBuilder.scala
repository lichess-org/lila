package lidraughts.search

import lidraughts.common.MaxPerPage
import lidraughts.common.paginator._

import play.api.libs.json.Writes

final class PaginatorBuilder[A, Q: Writes](
    searchApi: SearchReadApi[A, Q],
    maxPerPage: MaxPerPage
) {

  def apply(query: Q, page: Int): Fu[Paginator[A]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  private final class ESAdapter(query: Q) extends AdapterLike[A] {

    def nbResults = searchApi count query

    def slice(offset: Int, length: Int) =
      searchApi.search(query, From(offset), Size(length))
  }
}
