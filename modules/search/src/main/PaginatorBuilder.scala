package lila.search

import lila.common.config.MaxPerPage
import lila.common.paginator._

import play.api.libs.json.Writes

@scala.annotation.nowarn
final class PaginatorBuilder[A, Q: Writes](
    searchApi: SearchReadApi[A, Q],
    maxPerPage: MaxPerPage
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(query: Q, page: Int): Fu[Paginator[A]] =
    Paginator(
      adapter = new ESAdapter(query),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final private class ESAdapter(query: Q) extends AdapterLike[A] {

    def nbResults = searchApi count query

    def slice(offset: Int, length: Int) =
      searchApi.search(query, From(offset), Size(length))
  }
}
