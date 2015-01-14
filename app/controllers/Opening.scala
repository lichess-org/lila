package controllers

import scala.util.{ Try, Success, Failure }

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.opening.{ Generated, Opening => OpeningModel }
import lila.user.{ User => UserModel, UserRepo }
import views._
import views.html.opening.JsData

object Opening extends LilaController {

  private def env = Env.opening

  private def renderShow(opening: OpeningModel)(implicit ctx: Context) =
    env userInfos ctx.me map { infos =>
      views.html.opening.show(opening, infos, env.AnimationDuration)
    }

  def home = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) env.selector(ctx.me) zip (env userInfos ctx.me) map {
      case (opening, infos) => Ok(JsData(opening, infos,
        play = true,
        attempt = none,
        win = none,
        animationDuration = env.AnimationDuration)) as JSON
    }
    else env.selector(ctx.me) flatMap { opening =>
      renderShow(opening) map { Ok(_) }
    }
  }

  def show(id: OpeningModel.ID) = Open { implicit ctx =>
    OptionFuOk(env.api.opening find id)(renderShow)
  }

  def history = Auth { implicit ctx =>
    me =>
      XhrOnly {
        env userInfos me map { ui => Ok(views.html.opening.history(ui)) }
      }
  }

  private val attemptForm = Form(mapping(
    "found" -> number,
    "failed" -> number
  )(Tuple2.apply)(Tuple2.unapply))

  def attempt(id: OpeningModel.ID) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.opening find id) { opening =>
      attemptForm.bindFromRequest.fold(
        err => fuccess(BadRequest(err.errorsAsJson)),
        data => {
          val (found, failed) = data
          val win = found == opening.goal && failed == 0
          ctx.me match {
            case Some(me) => env.finisher(opening, me, win) flatMap {
              case (newAttempt, None) =>
                UserRepo byId me.id map (_ | me) flatMap { me2 =>
                  (env.api.opening find id) zip (env userInfos me2.some) map {
                    case (o2, infos) => Ok {
                      JsData(o2 | opening, infos,
                        play = false,
                        attempt = newAttempt.some,
                        win = none,
                        animationDuration = env.AnimationDuration)
                    }
                  }
                }
              case (oldAttempt, Some(win)) => env userInfos me.some map { infos =>
                Ok(JsData(opening, infos,
                  play = false,
                  attempt = oldAttempt.some,
                  win = win.some,
                  animationDuration = env.AnimationDuration))
              }
            }
            case None => fuccess {
              Ok(JsData(opening, none,
                play = false,
                attempt = none,
                win = win.some,
                animationDuration = env.AnimationDuration))
            }
          }
        }
      ) map (_ as JSON)
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
