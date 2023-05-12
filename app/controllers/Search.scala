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
    env.game.cached.nbTotal flatMap { nbGames =>
      if ctx.isAnon
      then
        negotiate(
          html = Unauthorized(html.search.login(nbGames)).toFuccess,
          api = _ => Unauthorized(jsonError("Login required")).toFuccess
        )
      else
        NoCrawlers:
          val page = p atLeast 1
          Reasonable(page, config.Max(100)):
            val cost = scala.math.sqrt(page.toDouble).toInt
            def limited = fuccess:
              val form = searchForm
                .bindFromRequest()
                .withError(
                  key = "",
                  message = "Please only send one request at a time per IP address"
                )
              TooManyRequests(html.search.index(form, none, nbGames))
            SearchRateLimitPerIP(ctx.ip, rateLimitedFu, cost = cost):
              SearchConcurrencyLimitPerIP(ctx.ip, limited = limited):
                negotiate(
                  html = searchForm
                    .bindFromRequest()
                    .fold(
                      failure => BadRequest(html.search.index(failure, none, nbGames)).toFuccess,
                      data =>
                        data.nonEmptyQuery
                          .?? { query =>
                            env.gameSearch.paginator(query, page) map some
                          }
                          .map: pager =>
                            Ok(html.search.index(searchForm fill data, pager, nbGames))
                          .recover: _ =>
                            InternalServerError("Sorry, we can't process that query at the moment")
                    ),
                  api = _ =>
                    searchForm
                      .bindFromRequest()
                      .fold(
                        _ =>
                          BadRequest {
                            jsonError("Could not process search query")
                          }.toFuccess,
                        data =>
                          data.nonEmptyQuery ?? { query =>
                            env.gameSearch.paginator(query, page) dmap some
                          } flatMap {
                            case Some(s) =>
                              env.api.userGameApi.jsPaginator(s) dmap {
                                Ok(_)
                              }
                            case None =>
                              BadRequest(jsonError("Could not process search query")).toFuccess
                          } recover { _ =>
                            InternalServerError(
                              jsonError("Sorry, we can't process that query at the moment")
                            )
                          }
                      )
                )
    }
