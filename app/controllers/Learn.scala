package controllers

import lila.app._

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import views.html

object Learn extends LilaController {

  val env = Env.learn

  import lila.learn.JSONHandlers._

  def index = Open { implicit ctx =>
    pageHit
    ctx.me.?? { me =>
      env.api.get(me) map { Json.toJson(_) } map some
    }.map { progress =>
      Ok(html.learn.index(progress))
    }
  }

  private val scoreForm = Form(mapping(
    "stage" -> nonEmptyText,
    "level" -> number,
    "score" -> number
  )(Tuple3.apply)(Tuple3.unapply))

  def score = AuthBody { implicit ctx => me =>
    implicit val body = ctx.body
    scoreForm.bindFromRequest.fold(
      err => BadRequest.fuccess, {
        case (stage, level, s) =>
          val score = lila.learn.StageProgress.Score(s)
          env.api.setScore(me, stage, level, score) >>
            Env.activity.write.learn(me.id, stage) inject Ok(Json.obj("ok" -> true))
      }
    )
  }

  def reset = AuthBody { implicit ctx => me =>
    env.api.reset(me) inject Ok(Json.obj("ok" -> true))
  }
}
