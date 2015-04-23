package controllers

import play.api.mvc.Action

import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Game => GameModel, GameRepo }
import play.api.http.ContentTypes
import views._

object Game extends LilaController {

  private def paginator = Env.game.paginator
  private def analysePaginator = Env.analyse.paginator
  private def cached = Env.game.cached
  private def searchEnv = Env.gameSearch
  def searchForm = searchEnv.forms.search

  def search(page: Int) = OpenBody { implicit ctx =>
    if (HTTPRequest.isBot(ctx.req)) notFound
    else Reasonable(page, 100) {
      implicit def req = ctx.body
      searchForm.bindFromRequest.fold(
        failure => Ok(html.game.search(failure)).fuccess,
        data => searchEnv.nonEmptyQuery(data) ?? { query =>
          searchEnv.paginator(query, page) map (_.some)
        } map { pager =>
          Ok(html.game.search(searchForm fill data, pager))
        }
      )
    }
  }

  def delete(gameId: String) = Auth { implicit ctx =>
    me =>
      OptionFuResult(GameRepo game gameId) { game =>
        if (game.pgnImport.flatMap(_.user) ?? (me.id==)) {
          Env.hub.actor.bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
          (GameRepo remove game.id) >>
            (lila.analyse.AnalysisRepo remove game.id) inject
            Redirect(routes.User.show(me.username))
        }
        else fuccess {
          Redirect(routes.Round.watcher(game.id, game.firstColor.name))
        }
      }
  }

  def export(user: String) = Auth { implicit ctx =>
    _ =>
      Env.security.forms.emptyWithCaptcha map {
        case (form, captcha) => Ok(html.game.export(user, form, captcha))
      }
  }

  def exportConfirm(user: String) = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      val userId = user.toLowerCase
      if (me.id == userId)
        Env.security.forms.empty.bindFromRequest.fold(
          err => Env.security.forms.anyCaptcha map { captcha =>
            BadRequest(html.game.export(userId, err, captcha))
          },
          _ => fuccess {
            play.api.Logger("export").info(s"$user from ${ctx.req.remoteAddress}")
            import org.joda.time.DateTime
            import org.joda.time.format.DateTimeFormat
            val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
            Ok.chunked(Env.game export userId).withHeaders(
              CONTENT_TYPE -> ContentTypes.TEXT,
              CONTENT_DISPOSITION -> ("attachment; filename=" + s"lichess_${me.username}_$date.pgn"))
          })
      else notFound
  }
}
