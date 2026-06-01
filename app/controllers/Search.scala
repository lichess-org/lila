package controllers
import lila.app.{ *, given }
import lila.core.i18n.Translate

final class Search(env: Env) extends LilaController(env):

  private def searchForm(using Translate) = env.gameSearch.forms.search

  def index(p: Int) = AuthBody { _ ?=> me ?=>
    env.game.cached.nbTotal.flatMap: nbGames =>
      val page = p.atLeast(1)
      Reasonable(page, Max(100)):
        val cost = scala.math.sqrt(page.toDouble).toInt
        def concurrencyLimited =
          val form = searchForm
            .bindFromRequest()
            .withError(key = "", message = "Please only send one request at a time")
          TooManyRequests.page(views.gameSearch.index(form, none, nbGames))
        limit.search(me.userId, rateLimited, cost = cost):
          limit.searchConcurrency(me.userId, concurrencyLimited):
            bindForm(searchForm)(
              failure =>
                negotiate(
                  BadRequest.page(views.gameSearch.index(failure, none, nbGames)),
                  BadRequest(jsonError("Could not process search query"))
                ),
              data =>
                data.nonEmptyQuery
                  .traverse: query =>
                    env.gameSearch.api
                      .validateAccounts(query, isGrantedOpt(_.GamesModView))
                      .flatMap:
                        _.so(env.gameSearch.paginator(query, page))
                  .flatMap: pager =>
                    negotiate(
                      Ok.page(views.gameSearch.index(searchForm.fill(data), pager, nbGames)),
                      pager.fold(BadRequest(jsonError("Could not process search query")).toFuccess): pager =>
                        env.game.userGameApi.jsPaginator(pager.toPaginator).dmap(Ok(_))
                    )
                  .recoverWith: _ =>
                    serverError("Sorry, we can't process that query at the moment")
            )
  }
