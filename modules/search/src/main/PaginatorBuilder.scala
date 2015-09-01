package lila.search

import lila.common.paginator._
import makeTimeout.large

final class PaginatorBuilder[A, Q <: Query](
    searchApi: SearchReadApi[A, Q],
    maxPerPage: Int) {

  def apply(query: Q, page: Int): Fu[Paginator[A]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage)

  def ids(query: Q, max: Int): Fu[List[String]] = ???

  private final class ESAdapter(query: Q) extends AdapterLike[A] {

    def nbResults = searchApi count query

    def slice(offset: Int, length: Int) =
      searchApi.search(query, From(offset), Size(length))
  }
}
