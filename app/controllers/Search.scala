package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import views._

final class Search(env: Env) extends LilaController(env) {

  def searchForm = env.gameSearch.forms.search

  private val SearchRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 50,
    duration = 5.minutes,
    key = "search.games.ip"
  )
  private val SearchConcurrencyLimitPerIP = new lila.memo.FutureConcurrencyLimit[IpAddress](
    key = "search.games.concurrency.ip",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  def index(p: Int) =
    OpenBody { implicit ctx =>
      NotForBots {
        val page = p atLeast 1
        Reasonable(page, 100) {
          val ip           = HTTPRequest lastRemoteAddress ctx.req
          val cost         = scala.math.sqrt(page.toDouble).toInt
          implicit def req = ctx.body
          env.game.cached.nbTotal flatMap { nbGames =>
            def limited =
              fuccess {
                val form = searchForm
                  .bindFromRequest()
                  .withError(
                    key = "",
                    message = "Please only send one request at a time per IP address"
                  )
                TooManyRequests(html.search.index(form, none, nbGames))
              }
            SearchRateLimitPerIP(ip, cost = cost) {
              SearchConcurrencyLimitPerIP(ip, limited = limited) {
                negotiate(
                  html = searchForm
                    .bindFromRequest()
                    .fold(
                      failure => Ok(html.search.index(failure, none, nbGames)).fuccess,
                      data =>
                        data.nonEmptyQuery ?? { query =>
                          env.gameSearch.paginator(query, page) map some
                        } map { pager =>
                          Ok(html.search.index(searchForm fill data, pager, nbGames))
                        } recover { _ =>
                          InternalServerError("Sorry, we can't process that query at the moment")
                        }
                    ),
                  api = _ =>
                    searchForm
                      .bindFromRequest()
                      .fold(
                        _ =>
                          Ok {
                            jsonError("Could not process search query")
                          }.fuccess,
                        data =>
                          data.nonEmptyQuery ?? { query =>
                            env.gameSearch.paginator(query, page) dmap some
                          } flatMap {
                            case Some(s) =>
                              env.api.userGameApi.jsPaginator(s) dmap {
                                Ok(_)
                              }
                            case None =>
                              BadRequest(jsonError("Could not process search query")).fuccess
                          } recover { _ =>
                            InternalServerError(jsonError("Sorry, we can't process that query at the moment"))
                          }
                      )
                )
              }
            }(rateLimitedFu)
          }
        }
      }
    }
}
