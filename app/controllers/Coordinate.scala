package controllers

import lidraughts.app._

object Coordinate extends LidraughtsController {

  private def env = Env.coordinate

  def home = Open { implicit ctx =>
    ctx.userId ?? { userId => env.api getScore userId map (_.some) } map { score =>
      views.html.coordinate.home(score)
    }
  }

  def score = AuthBody { implicit ctx => me =>
    implicit val body = ctx.body
    env.forms.score.bindFromRequest.fold(
      err => fuccess(BadRequest),
      data => env.api.addScore(me.id, data.isWhite, data.score)
    ) >> {
        env.api getScore me.id map { s =>
          Ok(views.html.coordinate.scoreCharts(s))
        }
      }
  }

  def color = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.color.bindFromRequest.fold(
      err => fuccess(BadRequest),
      value => Env.pref.api.setPref(
        me,
        (p: lidraughts.pref.Pref) => p.copy(coordColor = value),
        notifyChange = false
      ) inject Ok(())
    )
  }
}
