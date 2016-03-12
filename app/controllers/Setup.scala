package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import play.api.Play.current
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie }
import lila.game.{ GameRepo, Pov, AnonCookie }
import lila.setup.{ HookConfig, ValidFen }
import lila.user.UserRepo
import views._

object Setup extends LilaController with TheftPrevention {

  private def env = Env.setup

  private val PostRateLimit = new lila.memo.RateLimit(5, 1 minute, "setup post")

  def aiForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      env.forms aiFilled get("fen") map { form =>
        html.setup.ai(
          form,
          Env.fishnet.aiPerfApi.intRatings,
          form("fen").value flatMap ValidFen(getBool("strict")))
      }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#ai")
    }
  }

  def ai = process(env.forms.ai) { config =>
    implicit ctx =>
      env.processor ai config
  }

  def friendForm(userId: Option[String]) = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      env.forms friendFilled get("fen") flatMap { form =>
        val validFen = form("fen").value flatMap ValidFen(false)
        userId ?? UserRepo.named flatMap {
          case None => fuccess(html.setup.friend(form, none, none, validFen))
          case Some(user) => Challenge.restriction(user) map { error =>
            html.setup.friend(form, user.some, error, validFen)
          }
        }
      }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#friend")
    }
  }

  def friend(userId: Option[String]) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      PostRateLimit(req.remoteAddress) {
        env.forms.friend(ctx).bindFromRequest.fold(
          f => negotiate(
            html = Lobby.renderHome(Results.BadRequest),
            api = _ => fuccess(BadRequest(errorsAsJson(f)))
          ), {
            case config => userId ?? UserRepo.byId flatMap { destUser =>
              import lila.challenge.Challenge._
              val challenge = lila.challenge.Challenge.make(
                variant = config.variant,
                initialFen = config.fen,
                timeControl = config.makeClock map { c =>
                  TimeControl.Clock(c.limit, c.increment)
                } orElse config.makeDaysPerTurn.map {
                  TimeControl.Correspondence.apply
                } getOrElse TimeControl.Unlimited,
                mode = config.mode,
                color = config.color.name,
                challenger = (ctx.me, HTTPRequest sid req) match {
                  case (Some(user), _) => Right(user)
                  case (_, Some(sid))  => Left(sid)
                  case _               => Left("no_sid")
                },
                destUser = destUser,
                rematchOf = none)
              env.processor.saveFriendConfig(config) >>
                (Env.challenge.api create challenge) >> negotiate(
                  html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
                  api = _ => Challenge showChallenge challenge)
            }
          }
        )
      }
    }

  def hookForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
      env.forms.hookFilled(timeModeString = get("time")) map { html.setup.hook(_) }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#hook")
    }
  }

  private def hookResponse(hookId: String) =
    Ok(Json.obj(
      "ok" -> true,
      "hook" -> Json.obj("id" -> hookId))) as JSON

  def hook(uid: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    PostRateLimit(req.remoteAddress) {
      NoPlaybanOrCurrent {
        env.forms.hook(ctx).bindFromRequest.fold(
          err => negotiate(
            html = BadRequest(errorsAsJson(err).toString).fuccess,
            api = _ => BadRequest(errorsAsJson(err)).fuccess),
          config => (ctx.userId ?? Env.relation.api.fetchBlocking) flatMap {
            blocking =>
              env.processor.hook(config, uid, HTTPRequest sid req, blocking) map hookResponse recover {
                case e: IllegalArgumentException => BadRequest(jsonError(e.getMessage)) as JSON
              }
          }
        )
      }
    }
  }

  def like(uid: String, gameId: String) = Open { implicit ctx =>
    PostRateLimit(ctx.req.remoteAddress) {
      NoPlaybanOrCurrent {
        env.forms.hookConfig flatMap { config =>
          GameRepo game gameId map {
            _.fold(config)(config.updateFrom)
          } flatMap { config =>
            (ctx.userId ?? Env.relation.api.fetchBlocking) flatMap { blocking =>
              env.processor.hook(config, uid, HTTPRequest sid ctx.req, blocking) map hookResponse recover {
                case e: IllegalArgumentException => BadRequest(jsonError(e.getMessage)) as JSON
              }
            }
          }
        }
      }
    }
  }

  def filterForm = Open { implicit ctx =>
    env.forms.filterFilled map {
      case (form, filter) => html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f => fulogwarn(f.errors.toString) inject BadRequest(()),
      config => JsonOk(env.processor filter config inject config.render)
    )
  }

  def validateFen = Open { implicit ctx =>
    get("fen") flatMap ValidFen(getBool("strict")) match {
      case None    => BadRequest.fuccess
      case Some(v) => Ok(html.game.miniBoard(v.fen, v.color.name)).fuccess
    }
  }

  private def process[A](form: Context => Form[A])(op: A => BodyContext[_] => Fu[Pov]) =
    OpenBody { implicit ctx =>
      PostRateLimit(ctx.req.remoteAddress) {
        implicit val req = ctx.body
        form(ctx).bindFromRequest.fold(
          f => negotiate(
            html = Lobby.renderHome(Results.BadRequest),
            api = _ => fuccess(BadRequest(errorsAsJson(f)))
          ),
          config => op(config)(ctx) flatMap { pov =>
            negotiate(
              html = fuccess(redirectPov(pov)),
              api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { data =>
                Created(data) as JSON
              }
            )
          }
        )
      }
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) = {
    implicit val req = ctx.req
    val redir = Redirect(routes.Round.watcher(pov.game.id, "white"))
    if (ctx.isAuth) redir
    else redir withCookies LilaCookie.cookie(
      AnonCookie.name,
      pov.playerId,
      maxAge = AnonCookie.maxAge.some,
      httpOnly = false.some)
  }
}
