package controllers

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results }
import scala.concurrent.duration._

import chess.format.FEN
import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie, IpAddress }
import lila.game.{ Pov, AnonCookie }
import lila.setup.Processor.HookResult
import lila.setup.ValidFen
import lila.socket.Socket.Sri
import views._

final class Setup(
    env: Env,
    challengeC: => Challenge
) extends LilaController(env) with TheftPrevention {

  private def forms = env.setup.forms
  private def processor = env.setup.processor

  private[controllers] val PostRateLimit = new lila.memo.RateLimit[IpAddress](5, 1 minute,
    name = "setup post",
    key = "setup_post",
    enforce = env.net.rateLimit)

  def aiForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      forms aiFilled get("fen").map(FEN) map { form =>
        html.setup.forms.ai(
          form,
          env.fishnet.aiPerfApi.intRatings,
          form("fen").value flatMap ValidFen(getBool("strict"))
        )
      }
    } else fuccess {
      Redirect(s"${routes.Lobby.home}#ai")
    }
  }

  def ai = process(forms.ai) { config => implicit ctx =>
    processor ai config
  }

  def friendForm(userId: Option[String]) = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req)
      forms friendFilled get("fen").map(FEN) flatMap { form =>
        val validFen = form("fen").value flatMap ValidFen(false)
        userId ?? env.user.repo.named flatMap {
          case None => Ok(html.setup.forms.friend(form, none, none, validFen)).fuccess
          case Some(user) => env.challenge.granter(ctx.me, user, none) map {
            case Some(denied) => BadRequest(lila.challenge.ChallengeDenied.translated(denied))
            case None => Ok(html.setup.forms.friend(form, user.some, none, validFen))
          }
        }
      }
    else fuccess {
      Redirect(s"${routes.Lobby.home}#friend")
    }
  }

  def friend(userId: Option[String]) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
      forms.friend(ctx).bindFromRequest.fold(
        err => negotiate(
          html = keyPages.home(Results.BadRequest),
          api = _ => jsonFormError(err)
        ),
        config => userId ?? env.user.repo.enabledById flatMap { destUser =>
          destUser ?? { env.challenge.granter(ctx.me, _, config.perfType) } flatMap {
            case Some(denied) =>
              val message = lila.challenge.ChallengeDenied.translated(denied)
              negotiate(
                html = BadRequest(html.site.message.challengeDenied(message)).fuccess,
                api = _ => BadRequest(jsonError(message)).fuccess
              )
            case None =>
              import lila.challenge.Challenge._
              val challenge = lila.challenge.Challenge.make(
                variant = config.variant,
                initialFen = config.fen,
                timeControl = config.makeClock map { c =>
                  TimeControl.Clock(c)
                } orElse config.makeDaysPerTurn.map {
                  TimeControl.Correspondence.apply
                } getOrElse TimeControl.Unlimited,
                mode = config.mode,
                color = config.color.name,
                challenger = (ctx.me, HTTPRequest sid req) match {
                  case (Some(user), _) => Right(user)
                  case (_, Some(sid)) => Left(sid)
                  case _ => Left("no_sid")
                },
                destUser = destUser,
                rematchOf = none
              )
              processor.saveFriendConfig(config) >>
                (env.challenge.api create challenge) flatMap {
                  case true => negotiate(
                    html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
                    api = _ => challengeC showChallenge challenge
                  )
                  case false => negotiate(
                    html = fuccess(Redirect(routes.Lobby.home)),
                    api = _ => fuccess(BadRequest(jsonError("Challenge not created")))
                  )
                }
          }
        }
      )
    }
  }

  def hookForm = Open { implicit ctx =>
    NoBot {
      if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
        forms.hookFilled(timeModeString = get("time")) map { html.setup.forms.hook(_) }
      }
      else fuccess {
        Redirect(s"${routes.Lobby.home}#hook")
      }
    }
  }

  private def hookResponse(res: HookResult) = res match {
    case HookResult.Created(id) => Ok(Json.obj(
      "ok" -> true,
      "hook" -> Json.obj("id" -> id)
    )) as JSON
    case HookResult.Refused => BadRequest(jsonError("Game was not created"))
  }

  private val hookSaveOnlyResponse = Ok(Json.obj("ok" -> true))

  def hook(sri: String) = OpenBody { implicit ctx =>
    NoBot {
      implicit val req = ctx.body
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        NoPlaybanOrCurrent {
          forms.hook(ctx).bindFromRequest.fold(
            jsonFormError,
            userConfig => {
              val config = userConfig withinLimits ctx.me
              if (getBool("pool")) processor.saveHookConfig(config) inject hookSaveOnlyResponse
              else (ctx.userId ?? env.relation.api.fetchBlocking) flatMap {
                blocking =>
                  processor.hook(config, Sri(sri), HTTPRequest sid req, blocking) map hookResponse
              }
            }
          )
        }
      }
    }
  }

  def like(sri: String, gameId: String) = Open { implicit ctx =>
    NoBot {
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        NoPlaybanOrCurrent {
          for {
            config <- forms.hookConfig
            game <- env.game.gameRepo game gameId
            blocking <- ctx.userId ?? env.relation.api.fetchBlocking
            hookConfig = game.fold(config)(config.updateFrom)
            sameOpponents = game.??(_.userIds)
            hookResult <- processor.hook(hookConfig, Sri(sri), HTTPRequest sid ctx.req, blocking ++ sameOpponents)
          } yield hookResponse(hookResult)
        }
      }
    }
  }

  def filterForm = Open { implicit ctx =>
    forms.filterFilled map {
      case (form, filter) => html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f => {
        lila.log("setup").warn(f.errors.toString)
        BadRequest(()).fuccess
      },
      config => JsonOk(processor filter config inject config.render)
    )
  }

  def validateFen = Open { implicit ctx =>
    get("fen") flatMap ValidFen(getBool("strict")) match {
      case None => BadRequest.fuccess
      case Some(v) => Ok(html.game.bits.miniBoard(v.fen, v.color)).fuccess
    }
  }

  private def process[A](form: Context => Form[A])(op: A => BodyContext[_] => Fu[Pov]) =
    OpenBody { implicit ctx =>
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        implicit val req = ctx.body
        form(ctx).bindFromRequest.fold(
          err => negotiate(
            html = keyPages.home(Results.BadRequest),
            api = _ => jsonFormError(err)
          ),
          config => op(config)(ctx) flatMap { pov =>
            negotiate(
              html = fuccess(redirectPov(pov)),
              api = apiVersion => env.api.roundApi.player(pov, apiVersion) map { data =>
                Created(data) as JSON
              }
            )
          }
        )
      }
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) = {
    val redir = Redirect(routes.Round.watcher(pov.gameId, "white"))
    if (ctx.isAuth) redir
    else redir withCookies env.lilaCookie.cookie(
      AnonCookie.name,
      pov.playerId,
      maxAge = AnonCookie.maxAge.some,
      httpOnly = false.some
    )
  }
}
