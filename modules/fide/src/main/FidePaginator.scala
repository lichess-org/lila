package lila.fide

import reactivemongo.api.*

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class FidePaginator(repo: FideRepo)(using Executor):

  import repo.player.handler

  val maxPerPage = MaxPerPage(30)

  def best(page: Int): Fu[Paginator[FidePlayer]] =
    Paginator(
      adapter = new AdapterLike[FidePlayer]:
        def nbResults: Fu[Int] = fuccess(100 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          repo.playerColl
            .find(repo.player.selectActive)
            .sort($sort desc "standard")
            .skip(offset)
            .cursor[FidePlayer]()
            .list(length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
