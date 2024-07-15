package lila.fide

import reactivemongo.api.*
import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.*
import lila.db.paginator.{ Adapter, CachedAdapter }

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
            .sort($sort.desc("standard.top10Rating"))
            .skip(offset)
            .cursor[lila.fide.Federation]()
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
    val search = FidePlayer.tokenize(query).some.filter(_.size > 1)
    Paginator(
      adapter = search match
        case Some(search) =>
          val textScore = $doc("score" -> $doc("$meta" -> "textScore"))
          new Adapter[FidePlayer](
            collection = repo.playerColl,
            selector = $text(search),
            projection = textScore.some,
            sort = textScore ++ repo.player.sortStandard,
            _.sec
          )
        case _ =>
          new CachedAdapter[FidePlayer](
            nbResults = fuccess(100 * maxPerPage.value),
            adapter = new Adapter(
              collection = repo.playerColl,
              selector = repo.player.selectActive,
              projection = none,
              sort = repo.player.sortStandard,
              _.sec
            )
          )
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
