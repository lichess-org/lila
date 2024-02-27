package lila.fide

import reactivemongo.api.*

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class FidePaginator(coll: Coll)(using Executor):

  import FidePlayerApi.playerHandler

  val maxPerPage = MaxPerPage(30)

  def best(page: Int): Fu[Paginator[FidePlayer]] =
    Paginator(
      adapter = new AdapterLike[FidePlayer]:
        private val selector   = $doc("deleted" $ne true, "inactive" $ne true)
        def nbResults: Fu[Int] = fuccess(100 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          coll
            .find(selector)
            .sort($sort desc "standard")
            .skip(offset)
            .cursor[FidePlayer]()
            .list(length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
