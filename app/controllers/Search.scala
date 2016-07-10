package controllers

import lila.app._
import lila.api.Context
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo }
import play.api.http.ContentTypes
import views._

object Search extends LilaController {

  private def paginator = Env.game.paginator
  private def env = Env.gameSearch
  def searchForm = env.forms.search

  def index(page: Int) = OpenBody { implicit ctx =>
    NotForBots {
      Reasonable(page, 100) {
        implicit def req = ctx.body
        searchForm.bindFromRequest.fold(
          failure => Ok(html.search.index(failure)).fuccess,
          data => data.nonEmptyQuery ?? { query =>
            env.paginator(query, page) map (_.some)
          } map { pager =>
            Ok(html.search.index(searchForm fill data, pager))
          }
        )
      }
    }
  }

  def export = OpenBody { implicit ctx =>
    NotForBots {
      implicit def req = ctx.body
      searchForm.bindFromRequest.fold(
        failure => Ok(html.search.index(failure)).fuccess,
        data => data.nonEmptyQuery ?? { query =>
          env.api.ids(query, 5000) map { ids =>
            import org.joda.time.DateTime
            import org.joda.time.format.DateTimeFormat
            val date = (DateTimeFormat forPattern "yyyy-MM-dd") print DateTime.now
            Ok.chunked(Env.api.pgnDump exportGamesFromIds ids).withHeaders(
              CONTENT_TYPE -> ContentTypes.TEXT,
              CONTENT_DISPOSITION -> ("attachment; filename=" + s"lichess_search_$date.pgn"))
          }
        }
      )
    }
  }
}
