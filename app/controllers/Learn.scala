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

  private val stageForm = Form(mapping(
    "stage" -> nonEmptyText,
    "score" -> number
  )(Tuple2.apply)(Tuple2.unapply))

  def stage = AuthBody { implicit ctx =>
    me =>
      implicit val body = ctx.body
      stageForm.bindFromRequest.fold(
        err => BadRequest.fuccess,
        data => env.api.setScore(me, data._1, data._2) >>
          env.api.get(me).map { progress =>
            Ok(Json toJson progress) as JSON
          }
      )
  }
}
