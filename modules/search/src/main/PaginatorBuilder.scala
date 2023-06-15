package lila.search

import lila.common.config.MaxPerPage
import lila.common.paginator.*

import play.api.libs.json.Writes

final class PaginatorBuilder[A, Q: Writes](
    searchApi: SearchReadApi[A, Q],
    maxPerPage: MaxPerPage
)(using Executor):

  def apply(query: Q, page: Int): Fu[Paginator[A]] =
    Paginator(
      adapter = new AdapterLike[A]:
        def nbResults = searchApi count query
        def slice(offset: Int, length: Int) =
          searchApi.search(query, From(offset), Size(length))
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
