package controllers

import views.*

import play.api.i18n.Lang

import lila.app.{ given, * }
import lila.common.IpAddress
import lila.common.config

final class Search(env: Env) extends LilaController(env):

  def searchForm(using Lang) = env.gameSearch.forms.search

  private val SearchRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 50,
    duration = 5.minutes,
    key = "search.games.ip"
  )
  private val SearchConcurrencyLimitPerIP = lila.memo.FutureConcurrencyLimit[IpAddress](
    key = "search.games.concurrency.ip",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  def index(p: Int) = OpenBody:
    env.game.cached.nbTotal.flatMap: nbGames =>
      if ctx.isAnon then
        negotiate(
          Unauthorized.page(html.search.login(nbGames)),
          Unauthorized(jsonError("Login required"))
        )
      else
        NoCrawlers:
          val page = p atLeast 1
          Reasonable(page, config.Max(100)):
            val cost = scala.math.sqrt(page.toDouble).toInt
            def limited =
              val form = searchForm
                .bindFromRequest()
                .withError(
                  key = "",
                  message = "Please only send one request at a time per IP address"
                )
              TooManyRequests.page(html.search.index(form, none, nbGames))
            SearchRateLimitPerIP(ctx.ip, rateLimited, cost = cost):
              SearchConcurrencyLimitPerIP(ctx.ip, limited = limited):
                searchForm
                  .bindFromRequest()
                  .fold(
                    failure =>
                      negotiate(
                        BadRequest.page(html.search.index(failure, none, nbGames)),
                        BadRequest(jsonError("Could not process search query"))
                      ),
                    data =>
                      data.nonEmptyQuery
                        .soFu: query =>
                          env.gameSearch.api
                            .validateAccounts(query, isGrantedOpt(_.GamesModView))
                            .flatMap:
                              _.so(env.gameSearch.paginator(query, page))
                        .flatMap: pager =>
                          negotiate(
                            Ok.page(html.search.index(searchForm fill data, pager, nbGames)),
                            pager.fold(BadRequest(jsonError("Could not process search query")).toFuccess):
                              pager => env.api.userGameApi.jsPaginator(pager).dmap(Ok(_))
                          )
                        .recoverWith: _ =>
                          serverError("Sorry, we can't process that query at the moment")
                  )
