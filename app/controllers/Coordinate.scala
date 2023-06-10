package controllers

import play.api.mvc.Result

import lila.api.WebContext
import lila.app.{ given, * }

final class Coordinate(env: Env) extends LilaController(env):

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Coordinate.home)(serveHome)

  private def serveHome(using ctx: WebContext): Fu[Result] =
    ctx.userId ?? { userId =>
      env.coordinate.api getScore userId map (_.some)
    } map { score =>
      views.html.coordinate.show(score)
    }

  def score = AuthBody { ctx ?=> me =>
    env.coordinate.forms.score
      .bindFromRequest()
      .fold(
        _ => fuccess(BadRequest),
        data => env.coordinate.api.addScore(me.id, data.mode, data.color, data.score) inject Ok(())
      )
  }
