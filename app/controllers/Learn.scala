package controllers

import lila.app._

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import views.html

final class Learn(env: Env) extends LilaController(env) {

  import lila.learn.JSONHandlers._

  def index =
    Open { implicit ctx =>
      pageHit
      negotiate(
        html = ctx.me
          .?? { me =>
            env.learn.api.get(me) map { Json.toJson(_) } map some
          }
          .map { progress =>
            Ok(html.learn.index(progress, ctx.pref))
          },
        api = _ =>
          ctx.me.fold(BadRequest("Not logged in").fuccess) { me =>
            env.learn.api.get(me) map { Json.toJson(_) } dmap { JsonOk(_) }
          }
      )
    }

  private val scoreForm = Form(
    mapping(
      "stage" -> nonEmptyText,
      "level" -> number,
      "score" -> number
    )(lila.learn.ScoreEntry.apply)(lila.learn.ScoreEntry.unapply)
  )

  def score =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      scoreForm
        .bindFromRequest()
        .fold(
          _ => BadRequest.fuccess,
          scores => env.learn.api.setScore(me, scores) inject Ok(Json.obj("ok" -> true))
        )
    }

  private val scoresForm = Form(
    mapping(
      "scores" -> list(
        mapping(
          "stage" -> nonEmptyText,
          "level" -> number,
          "score" -> number
        )(lila.learn.ScoreEntry.apply)(lila.learn.ScoreEntry.unapply)
      )
    )(identity)(Some(_))
  )

  def scores =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      scoresForm
        .bindFromRequest()
        .fold(
          _ => BadRequest.fuccess,
          scores => env.learn.api.setScores(me, scores) inject Ok(Json.obj("ok" -> true))
        )
    }

  def reset =
    AuthBody { _ => me =>
      env.learn.api.reset(me) inject Ok(Json.obj("ok" -> true))
    }
}
