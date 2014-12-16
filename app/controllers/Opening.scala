package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.opening.{ Generated, Opening => OpeningModel }
import lila.user.{ User => UserModel, UserRepo }
import views._
// import views.html.opening.JsData

object Opening extends LilaController {

  private def env = Env.opening

  private def renderShow(opening: OpeningModel)(implicit ctx: Context) =
    env userInfos ctx.me map { infos =>
      views.html.opening.show(opening, infos)
    }

  def home = Open { implicit ctx =>
    env.selector(ctx.me) flatMap { opening =>
      renderShow(opening) map { Ok(_) }
    }
  }

  def show(id: OpeningModel.ID) = Open { implicit ctx =>
    OptionFuOk(env.api.opening find id)(renderShow)
  }

  def importOne = Action.async(parse.json) { implicit req =>
    env.api.opening.importOne(req.body, ~get("token", req)) map { id =>
      Ok("kthxbye " + {
        val url = s"http://lichess.org/training/opening/$id"
        play.api.Logger("opening import").info(s"${req.remoteAddress} $url")
        url
      })
    } recover {
      case e =>
        play.api.Logger("opening import").warn(e.getMessage)
        BadRequest(e.getMessage)
    }
  }
}
