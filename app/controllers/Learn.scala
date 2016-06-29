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

  private val levelForm = Form(mapping(
    "level" -> nonEmptyText,
    "score" -> number
  )(Tuple2.apply)(Tuple2.unapply))

  def level = AuthBody { implicit ctx =>
    me =>
      implicit val body = ctx.body
      levelForm.bindFromRequest.fold(
        err => BadRequest.fuccess,
        data => env.api.setScore(me, data._1, data._2) inject Ok
      )
  }
}
