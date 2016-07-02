package controllers

import lila.app._

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import views.html

object Learn extends LilaController {

  val env = Env.learn

  import lila.learn.JSONHandlers._

  def index = Auth { implicit ctx =>
    me =>
      env.api.get(me) map { progress =>
        Ok(html.learn.index(me, Json toJson progress))
      }
  }

  private val scoreForm = Form(mapping(
    "stage" -> nonEmptyText,
    "level" -> number,
    "score" -> number
  )(Tuple3.apply)(Tuple3.unapply))

  def score = AuthBody { implicit ctx =>
    me =>
      implicit val body = ctx.body
      scoreForm.bindFromRequest.fold(
        err => BadRequest.fuccess, {
          case (stage, level, s) =>
            val score = lila.learn.StageProgress.Score(s)
            env.api.setScore(me, stage, level, score) >>
              env.api.get(me).map { progress =>
                Ok(Json toJson progress) as JSON
              }
        })
  }
}
