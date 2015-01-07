package controllers

import scala.util.{ Try, Success, Failure }

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.opening.{ Generated, Opening => OpeningModel }
import lila.user.{ User => UserModel, UserRepo }
import lila.common.HTTPRequest
import views._
import views.html.opening.JsData

object Opening extends LilaController {

  private def env = Env.opening

  private def renderShow(opening: OpeningModel)(implicit ctx: Context) =
    env userInfos ctx.me map { infos =>
      views.html.opening.show(opening, infos)
    }

  def home = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) env.selector(ctx.me) zip (env userInfos ctx.me) map {
      case (opening, infos) => Ok(JsData(opening, infos, true)) as JSON
    }
    else env.selector(ctx.me) flatMap { opening =>
      renderShow(opening) map { Ok(_) }
    }
  }

  def show(id: OpeningModel.ID) = Open { implicit ctx =>
    OptionFuOk(env.api.opening find id)(renderShow)
  }

  private val attemptForm = Form(mapping(
    "found" -> number,
    "failed" -> number
  )(Tuple2.apply)(Tuple2.unapply))

  def attempt(id: OpeningModel.ID) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.opening find id) { opening =>
      attemptForm.bindFromRequest.fold(
        err => fuccess(BadRequest(err.errorsAsJson)), {
          case (found, failed) => ctx.me match {
            case Some(me) => env.finisher(opening, me, found, failed) flatMap { attempt =>
              UserRepo byId me.id map (_ | me) flatMap { me2 =>
                env.api.opening find id zip
                  (env userInfos me2.some) map {
                    case (o2, infos) => Ok {
                      JsData(o2 | opening, infos, false)
                    }
                  }
              }
            }
            case None => fuccess {
              Ok(JsData(opening, none, false))
            }
          }
        }) map (_ as JSON)
    }
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
