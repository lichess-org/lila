package controllers

import scala.util.{ Try, Success, Failure }

import play.api.data._, Forms._
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.Play.current
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

  private def identify(opening: OpeningModel) =
    env.api.identify(opening.fen, 5)

  private def renderShow(opening: OpeningModel)(implicit ctx: Context) =
    env userInfos ctx.me zip identify(opening) map {
      case (infos, identified) =>
        views.html.opening.show(opening, identified, infos, env.AnimationDuration)
    }

  private def makeData(
    opening: OpeningModel,
    infos: Option[UserInfos],
    play: Boolean,
    attempt: Option[Attempt],
    win: Option[Boolean])(implicit ctx: Context): Fu[Result] =
    identify(opening) map { identified =>
      Ok(JsData(
        opening,
        identified,
        infos,
        play = play,
        attempt = attempt,
        win = win,
        animationDuration = env.AnimationDuration)) as JSON
    }

  private val noMoreOpeningJson = jsonError("No more openings for you!")

  def home = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) env.selector(ctx.me) zip (env userInfos ctx.me) flatMap {
      case (Some(opening), infos) => makeData(opening, infos, true, none, none)
      case (None, _)              => NotFound(noMoreOpeningJson).fuccess
    }
    else env.selector(ctx.me) flatMap {
      case Some(opening) => renderShow(opening) map { Ok(_) }
      case None          => fuccess(Ok(html.opening.noMore()))
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
        err => fuccess(BadRequest(errorsAsJson(err)) as JSON),
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
}
