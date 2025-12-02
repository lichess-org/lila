package lila.fide

import reactivemongo.api.*
import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.{ *, given }
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.core.fide.FidePlayerOrder

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

  def federationPlayers(fed: Federation, page: Int)(using Option[Me]): Fu[Paginator[FidePlayer.WithFollow]] =
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
    ).flatMap(addFollows)

  def ordered(page: Int, query: String, order: FidePlayerOrder)(using
      me: Option[Me]
  ): Fu[Paginator[FidePlayer.WithFollow]] =
    val search = FidePlayer.tokenize(query).some.filter(_.size > 1)
    Paginator(
      adapter = search match
        case Some(search) =>
          val textScore = $doc("score" -> $doc("$meta" -> "textScore"))
          Adapter[FidePlayer](
            collection = repo.playerColl,
            selector = $text(search),
            projection = textScore.some,
            sort = textScore ++ repo.player.sortStandard, // don't touch, hits FTS index with standard
            _.sec
          )
        case _ =>
          val plentyOfResults = fuccess(100 * maxPerPage.value)
          me match
            case Some(me) if order == FidePlayerOrder.follow =>
              new AdapterLike[FidePlayer]:
                def nbResults: Fu[Int] = plentyOfResults
                def slice(offset: Int, length: Int): Fu[Seq[FidePlayer]] =
                  repo.followerColl
                    .aggregateList(length, _.sec): framework =>
                      import framework.*
                      Match($doc("u" -> me.userId)) -> List(
                        Project($doc("_id" -> false, "p" -> true)),
                        PipelineOperator:
                          $lookup.simple(from = repo.playerColl, as = "player", local = "p", foreign = "_id")
                        ,
                        Unwind("player"),
                        ReplaceRootField("player"),
                        Sort(Descending(FidePlayerOrder.default.key)),
                        Skip(offset),
                        Limit(length)
                      )
                    .map:
                      _.flatMap(repo.player.handler.readOpt)
            case _ =>
              CachedAdapter(
                Adapter[FidePlayer](
                  collection = repo.playerColl,
                  selector = repo.player.selectActive,
                  projection = none,
                  sort = repo.player.sortBy(order),
                  _.sec
                ),
                plentyOfResults
              )
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    ).flatMap(addFollows)

  private def addFollows(
      pager: Paginator[FidePlayer]
  )(using me: Option[Me]): Fu[Paginator[FidePlayer.WithFollow]] =
    pager.mapFutureList: players =>
      me.fold(fuccess(players.map(FidePlayer.WithFollow(_, false)))): me =>
        repo.follower.withFollows(players, me.userId)
