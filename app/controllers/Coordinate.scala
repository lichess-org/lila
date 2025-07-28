package controllers

import play.api.mvc.Result

import lila.app.{ *, given }

final class Coordinate(env: Env) extends LilaController(env):

  def home = Open(serveHome)
  def homeLang = LangPage(routes.Coordinate.home)(serveHome)

  private def serveHome(using ctx: Context): Fu[Result] =
    ctx.userId
      .so: userId =>
        env.coordinate.api.getScore(userId).map(_.some)
      .flatMap: score =>
        Ok.page(views.coordinate.show(score))

  def score = AuthBody { ctx ?=> me ?=>
    bindForm(env.coordinate.forms.score)(
      _ => fuccess(BadRequest),
      data => env.coordinate.api.addScore(data.mode, data.color, data.score).inject(Ok(()))
    )
  }
