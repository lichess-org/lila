package lila.fide

import reactivemongo.api.*

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class FidePaginator(repo: FideRepo)(using Executor):

  import repo.player.given
  import repo.federation.given

  val maxPerPage = MaxPerPage(30)

  def federations(page: Int): Fu[Paginator[Federation]] =
    Paginator(
      adapter = new AdapterLike[Federation]:
        def nbResults: Fu[Int] = fuccess(Federation.names.size)
        def slice(offset: Int, length: Int) =
          repo.federationColl
            .find($empty)
            .sort($sort desc "standard.top10Rating")
            .skip(offset)
            .cursor[Federation]()
            .list(length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def federationPlayers(fed: Federation, page: Int): Fu[Paginator[FidePlayer]] =
    Paginator(
      adapter = new AdapterLike[FidePlayer]:
        def nbResults: Fu[Int] = fuccess(100 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          repo.playerColl
            .find(repo.player.selectActive ++ repo.player.selectFed(fed.id))
            .sort(repo.player.sortStandard)
            .skip(offset)
            .cursor[FidePlayer]()
            .list(length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def best(page: Int, query: String): Fu[Paginator[FidePlayer]] =
    Paginator(
      adapter = new AdapterLike[FidePlayer]:
        def nbResults: Fu[Int] = fuccess(100 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          val searchSelect: Bdoc = query.toLowerCase.some.filter(_.size > 1).so($text(_))
          repo.playerColl
            .find:
              repo.player.selectActive ++ searchSelect
            .sort(repo.player.sortStandard)
            .skip(offset)
            .cursor[FidePlayer]()
            .list(length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
