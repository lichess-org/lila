package controllers

import scala.concurrent.duration._

import lidraughts.app._
import lidraughts.common.{ HTTPRequest, IpAddress }
import views._

object Search extends LidraughtsController {

  private def env = Env.gameSearch
  def searchForm = env.forms.search

  private val RateLimitGlobal = new lidraughts.memo.RateLimit[String](
    credits = 50,
    duration = 1 minute,
    name = "search games global",
    key = "search.games.global"
  )

  private val RateLimitPerIP = new lidraughts.memo.RateLimit[IpAddress](
    credits = 50,
    duration = 5 minutes,
    name = "search games per IP",
    key = "search.games.ip"
  )

  private val LinearLimitPerIP = new lidraughts.memo.LinearLimit(
    name = "search games per IP",
    key = "search.games.ip",
    ttl = 5 minutes
  )

  def index(p: Int) = OpenBody { implicit ctx =>
    NotForBots {
      val page = p atLeast 1
      Reasonable(page, 100) {
        val ip = HTTPRequest lastRemoteAddress ctx.req
        val cost = scala.math.sqrt(page).toInt
        implicit def req = ctx.body
        Env.game.cached.nbTotal flatMap { nbGames =>
          LinearLimitPerIP(ip.value) {
            RateLimitPerIP(ip, cost = cost) {
              RateLimitGlobal("-", cost = cost) {
                negotiate(
                  html = searchForm.bindFromRequest.fold(
                    failure => Ok(html.search.index(failure, none, nbGames)).fuccess,
                    data => data.nonEmptyQuery ?? { query =>
                      env.paginator(query, page) map (_.some)
                    } map { pager =>
                      Ok(html.search.index(searchForm fill data, pager, nbGames))
                    }
                  ),
                  api = _ => searchForm.bindFromRequest.fold(
                    failure => Ok(jsonError("Could not process search query")).fuccess,
                    data => data.nonEmptyQuery ?? { query =>
                      env.paginator(query, page) map (_.some)
                    } flatMap {
                      case Some(s) =>
                        Env.api.userGameApi.jsPaginator(s) map {
                          Ok(_)
                        }
                      case None =>
                        BadRequest(jsonError("Could not process search query")).fuccess
                    }
                  )
                )
              }
            }
          } | fuccess {
            val form = searchForm.bindFromRequest.withError(
              key = "",
              message = "Please only send one request at a time per IP address"
            )
            TooManyRequest(html.search.index(form, none, nbGames))
          }
        }
      }
    }
  }

  def export = OpenBody { implicit ctx =>
    NotForBots {
      implicit def req = ctx.body
      searchForm.bindFromRequest.fold(
        failure => Env.game.cached.nbTotal map { nbGames =>
          Ok(html.search.index(failure, none, nbGames))
        },
        data => data.nonEmptyQuery ?? { query =>
          env.api.ids(query, 5000) map { ids =>
            import org.joda.time.DateTime
            import org.joda.time.format.DateTimeFormat
            val date = (DateTimeFormat forPattern "yyyy-MM-dd") print DateTime.now
            Ok.chunked(Env.api.pdnDump exportGamesFromIds ids).withHeaders(
              CONTENT_TYPE -> pdnContentType,
              CONTENT_DISPOSITION -> ("attachment; filename=" + s"lidraughts_search_$date.pdn")
            )
          }
        }
      )
    }
  }
}
