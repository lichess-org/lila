package lila.search

import scalalib.paginator.*

final class PaginatorBuilder[A, Q](
    searchApi: SearchReadApi[A, Q],
    maxPerPage: MaxPerPage
)(using Executor):

  def apply(query: Q, page: Int): Fu[Paginator[A]] =
    Paginator(
      adapter = new AdapterLike[A]:
        def nbResults = searchApi.count(query).dmap(_.toInt)
        def slice(offset: Int, length: Int) =
          searchApi.search(query, From(offset), Size(length))
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
