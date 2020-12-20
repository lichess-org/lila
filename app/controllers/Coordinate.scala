package controllers

import lila.app._

final class Coordinate(env: Env) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      ctx.userId ?? { userId =>
        env.coordinate.api getScore userId map (_.some)
      } map { score =>
        views.html.coordinate.home(score)
      }
    }

  def score =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      env.coordinate.forms.score
        .bindFromRequest()
        .fold(
          _ => fuccess(BadRequest),
          data => env.coordinate.api.addScore(me.id, data.isWhite, data.score)
        ) >> {
        env.coordinate.api getScore me.id map { s =>
          Ok(views.html.coordinate.scoreCharts(s))
        }
      }
    }

  def color =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      env.coordinate.forms.color
        .bindFromRequest()
        .fold(
          _ => fuccess(BadRequest),
          value =>
            env.pref.api.setPref(
              me,
              (p: lila.pref.Pref) => p.copy(coordColor = value)
            ) inject Ok(())
        )
    }
}
