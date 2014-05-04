package controllers

import play.api.mvc._

import lila.app._
import views._

object Coordinate extends LilaController {

  private def env = Env.coordinate

  def home = Open { implicit ctx =>
    ctx.userId ?? { userId => env.api getScore userId map (_.some) } map { score =>
      views.html.coordinate.home(score)
    }
  }

  def score = AuthBody { implicit ctx =>
    me =>
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

  def color = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      env.forms.color.bindFromRequest.fold(
        err => fuccess(BadRequest),
        value => Env.pref.api.setPref(me, (p: lila.pref.Pref) => p.copy(coordColor = value)) inject Ok()
      )
  }
}
