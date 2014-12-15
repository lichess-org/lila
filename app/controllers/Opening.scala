package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.opening.{ Generated, Opening => OpeningModel }
import lila.user.{ User => UserModel, UserRepo }
import views._
// import views.html.puzzle.JsData

object Opening extends LilaController {

  private def env = Env.opening

  def importOne = Action.async(parse.json) { implicit req =>
    env.api.opening.importOne(req.body, ~get("token", req)) map { id =>
      Ok("kthxbye " + {
        val url = s"http://lichess.org/training/opening/$id"
        play.api.Logger("opening import").info(s"${req.remoteAddress} $url")
        url
      }.mkString(" "))
    } recover {
      case e =>
        play.api.Logger("opening import").warn(e.getMessage)
        BadRequest(e.getMessage)
    }
  }
}
