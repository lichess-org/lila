package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.fishnet.JsonApi._

object Fishnet extends LilaController {

  private def env = Env.fishnet
  private def api = env.api

  def acquire = Action.async(BodyParsers.parse.json) { req =>
    req.body.validate[lila.fishnet.JsonApi.Acquire].fold(
      err => BadRequest(Json.obj("error" -> JsError.toJson(err))).fuccess,
      acquire => api.authenticateClient(acquire) flatMap {
        case None => Unauthorized.fuccess
        case Some(client) => api acquire client map {
          case None       => NotFound
          case Some(work) => Ok(Json toJson work)
        }
      })
  }
}
