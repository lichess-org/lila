package controllers

import play.api.mvc.Action

import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo }
import play.api.http.ContentTypes
import views._

object Search extends LilaController {

  private def paginator = Env.game.paginator
  private def searchEnv = Env.gameSearch
  def searchForm = searchEnv.forms.search

  def index(page: Int) = OpenBody { implicit ctx =>
    if (HTTPRequest.isBot(ctx.req)) notFound
    else Reasonable(page, 100) {
      implicit def req = ctx.body
      searchForm.bindFromRequest.fold(
        failure => Ok(html.search.index(failure)).fuccess,
        data => searchEnv.nonEmptyQuery(data) ?? { query =>
          searchEnv.paginator(query, page) map (_.some)
        } map { pager =>
          Ok(html.search.index(searchForm fill data, pager))
        }
      )
    }
  }
}
