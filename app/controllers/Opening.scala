package controllers

import scala.util.{ Try, Success, Failure }

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.opening.{ Generated, Opening => OpeningModel, UserInfos, Attempt }
import lila.user.{ User => UserModel, UserRepo }
import views._
import views.html.opening.JsData

object Opening extends LilaController {

  private def env = Env.opening

  private def renderShow(opening: OpeningModel)(implicit ctx: Context) =
    env userInfos ctx.me zip
      (env.api.name find opening.fen) map {
        case (infos, names) =>
          views.html.opening.show(opening, names, infos, env.AnimationDuration)
      }

  private def makeData(
    opening: OpeningModel,
    infos: Option[UserInfos],
    play: Boolean,
    attempt: Option[Attempt],
    win: Option[Boolean])(implicit ctx: Context): Fu[Result] =
    env.api.name find opening.fen map { names =>
      Ok(JsData(
        opening,
        names,
        infos,
        play = play,
        attempt = attempt,
        win = win,
        animationDuration = env.AnimationDuration)) as JSON
    }

  def home = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) env.selector(ctx.me) zip (env userInfos ctx.me) flatMap {
      case (opening, infos) => makeData(opening, infos, true, none, none)
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
        err => fuccess(BadRequest(err.errorsAsJson) as JSON),
        data => {
          val (found, failed) = data
          val win = found == opening.goal && failed == 0
          ctx.me match {
            case Some(me) => env.finisher(opening, me, win) flatMap {
              case (newAttempt, None) =>
                UserRepo byId me.id map (_ | me) flatMap { me2 =>
                  (env.api.opening find id) zip (env userInfos me2.some) flatMap {
                    case (o2, infos) =>
                      makeData(o2 | opening, infos, false, newAttempt.some, none)
                  }
                }
              case (oldAttempt, Some(win)) => env userInfos me.some flatMap { infos =>
                makeData(opening, infos, false, oldAttempt.some, win.some)
              }
            }
            case None => makeData(opening, none, false, none, win.some)
          }
        }
      )
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
