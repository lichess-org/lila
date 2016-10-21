package controllers

import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo }
import play.api.http.ContentTypes
import views._

object Search extends LilaController {

  private def paginator = Env.game.paginator
  private def env = Env.gameSearch
  def searchForm = env.forms.search

  private val RateLimitGlobal = new lila.memo.RateLimit(
    credits = 50,
    duration = 1 minute,
    name = "search games global",
    key = "search.games.global")

  private val RateLimitPerIP = new lila.memo.RateLimit(
    credits = 50,
    duration = 5 minutes,
    name = "search games per IP",
    key = "search.games.ip")

  def index(p: Int) = OpenBody { implicit ctx =>
    NotForBots {
      val page = p min 100 max 1
      Reasonable(page, 100) {
        val ip = HTTPRequest lastRemoteAddress ctx.req
        val cost = scala.math.sqrt(page).toInt
        RateLimitPerIP(ip, cost = cost) {
          RateLimitGlobal("-", cost = cost) {
            Env.game.cached.nbTotal flatMap { nbGames =>
              implicit def req = ctx.body
              searchForm.bindFromRequest.fold(
                failure => Ok(html.search.index(failure, none, nbGames)).fuccess,
                data => data.nonEmptyQuery ?? { query =>
                  env.paginator(query, page) map (_.some)
                } map { pager =>
                  Ok(html.search.index(searchForm fill data, pager, nbGames))
                }
              )
            }
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
            Ok.chunked(Env.api.pgnDump exportGamesFromIds ids).withHeaders(
              CONTENT_TYPE -> pgnContentType,
              CONTENT_DISPOSITION -> ("attachment; filename=" + s"lichess_search_$date.pgn"))
          }
        }
      )
    }
  }
}
