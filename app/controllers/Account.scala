package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import lila.user.{ User => UserModel, UserRepo, TotpSecret }
import UserModel.{ ClearPassword, TotpToken, PasswordAndToken }
import views.html

object Account extends LilaController {

  private def env = Env.user
  private def relationEnv = Env.relation

  def profile = Auth { implicit ctx => me =>
    Ok(html.account.profile(me, env.forms profileOf me)).fuccess
  }

  def username = Auth { implicit ctx => me =>
    Ok(html.account.username(me, env.forms usernameOf me)).fuccess
  }

  def profileApply = AuthBody { implicit ctx => me =>
    implicit val req: Request[_] = ctx.body
    FormFuResult(env.forms.profile) { err =>
      fuccess(html.account.profile(me, err))
    } { profile =>
      UserRepo.setProfile(me.id, profile) inject Redirect(routes.User show me.username)
    }
  }

  def usernameApply = AuthBody { implicit ctx => me =>
    implicit val req: Request[_] = ctx.body
    FormFuResult(env.forms.username(me)) { err =>
      fuccess(html.account.username(me, err))
    } { username =>
      UserRepo.setUsernameCased(me.id, username) inject Redirect(routes.User show me.username) recoverWith {
        case e => fuccess(html.account.username(me, env.forms.username(me).withGlobalError(e.getMessage)))
      }
    }
  }

  def info = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => {
        lila.mon.http.response.accountInfo.count()
        relationEnv.api.countFollowers(me.id) zip
          relationEnv.api.countFollowing(me.id) zip
          Env.pref.api.getPref(me) zip
          lila.game.GameRepo.urgentGames(me) zip
          Env.challenge.api.countInFor.get(me.id) zip
          Env.playban.api.currentBan(me.id) map {
            case nbFollowers ~ nbFollowing ~ prefs ~ povs ~ nbChallenges ~ playban =>
              Env.current.system.lilaBus.publish(lila.user.User.Active(me), 'userActive)
              Ok {
                import lila.pref.JsonView._
                Env.user.jsonView(me) ++ Json.obj(
                  "prefs" -> prefs,
                  "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying),
                  "nbFollowing" -> nbFollowing,
                  "nbFollowers" -> nbFollowers,
                  "nbChallenges" -> nbChallenges
                ).add("kid" -> me.kid)
                  .add("troll" -> me.troll)
                  .add("playban" -> playban)
              }
          }
      }.mon(_.http.response.accountInfo.time)
    )
  }

  def nowPlaying = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => doNowPlaying(me, ctx.req)
    )
  }

  def apiMe = Scoped() { _ => me =>
    Env.api.userApi.extended(me, me.some) map { JsonOk(_) }
  }

  def apiNowPlaying = Scoped() { req => me =>
    doNowPlaying(me, req)
  }

  private def doNowPlaying(me: lila.user.User, req: RequestHeader) =
    lila.game.GameRepo.urgentGames(me) map { povs =>
      val nb = (getInt("nb", req) | 9) atMost 50
      Ok(Json.obj("nowPlaying" -> JsArray(povs take nb map Env.api.lobbyApi.nowPlaying)))
    }

  def dasher = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => Env.pref.api.getPref(me) map { prefs =>
        Ok {
          import lila.pref.JsonView._
          lila.common.LightUser.lightUserWrites.writes(me.light) ++ Json.obj(
            "coach" -> isGranted(_.Coach),
            "prefs" -> prefs
          )
        }
      }
    )
  }

  def passwd = Auth { implicit ctx => me =>
    env.forms passwd me map { form =>
      Ok(html.account.passwd(form))
    }
  }

  def passwdApply = AuthBody { implicit ctx => me =>
    controllers.Auth.HasherRateLimit(me.username, ctx.req) { _ =>
      implicit val req = ctx.body
      env.forms passwd me flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.passwd(err))
        } { data =>
          Env.user.authenticator.setPassword(me.id, ClearPassword(data.newPasswd1)) inject
            Redirect(s"${routes.Account.passwd}?ok=1")
        }
      }
    }
  }

  private def emailForm(user: UserModel) = UserRepo email user.id flatMap {
    Env.security.forms.changeEmail(user, _)
  }

  def email = Auth { implicit ctx => me =>
    if (getBool("check")) Ok(renderCheckYourEmail).fuccess
    else emailForm(me) map { form =>
      Ok(html.account.email(me, form))
    }
  }

  def apiEmail = Scoped(_.Email.Read) { _ => me =>
    UserRepo email me.id map {
      _ ?? { email =>
        JsonOk(Json.obj("email" -> email.value))
      }
    }
  }

  def renderCheckYourEmail(implicit ctx: Context) =
    html.auth.checkYourEmail(lila.security.EmailConfirm.cookie get ctx.req)

  def emailApply = AuthBody { implicit ctx => me =>
    controllers.Auth.HasherRateLimit(me.username, ctx.req) { _ =>
      implicit val req = ctx.body
      Env.security.forms.preloadEmailDns >> emailForm(me).flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.email(me, err))
        } { data =>
          val email = Env.security.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
          val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, email.acceptable)
          controllers.Auth.EmailConfirmRateLimit(newUserEmail, ctx.req) {
            Env.security.emailChange.send(me, newUserEmail.email) inject Redirect {
              s"${routes.Account.email}?check=1"
            }
          }
        }
      }
    }
  }

  def emailConfirm(token: String) = Open { implicit ctx =>
    Env.security.emailChange.confirm(token) flatMap {
      _ ?? { user =>
        controllers.Auth.authenticateUser(user, result = Some { _ =>
          Redirect(s"${routes.Account.email}?ok=1")
        })
      }
    }
  }

  def emailConfirmHelp = OpenBody { implicit ctx =>
    import lila.security.EmailConfirm.Help._
    ctx.me match {
      case Some(me) =>
        Redirect(routes.User.show(me.username)).fuccess
      case None if get("username").isEmpty =>
        Ok(html.account.emailConfirmHelp(helpForm, none)).fuccess
      case None =>
        implicit val req = ctx.body
        helpForm.bindFromRequest.fold(
          err => BadRequest(html.account.emailConfirmHelp(err, none)).fuccess,
          username => getStatus(username) map { status =>
            Ok(html.account.emailConfirmHelp(helpForm fill username, status.some))
          }
        )
    }
  }

  def twoFactor = Auth { implicit ctx => me =>
    if (me.totpSecret.isDefined)
      Env.security.forms.disableTwoFactor(me) map { form =>
        html.account.twoFactor.disable(me, form)
      }
    else
      Env.security.forms.setupTwoFactor(me) map { form =>
        html.account.twoFactor.setup(me, form)
      }
  }

  def setupTwoFactor = AuthBody { implicit ctx => me =>
    controllers.Auth.HasherRateLimit(me.username, ctx.req) { _ =>
      implicit val req = ctx.body
      val currentSessionId = ~Env.security.api.reqSessionId(ctx.req)
      Env.security.forms.setupTwoFactor(me) flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.twoFactor.setup(me, err))
        } { data =>
          UserRepo.setupTwoFactor(me.id, TotpSecret(data.secret)) >>
            lila.security.Store.closeUserExceptSessionId(me.id, currentSessionId) >>
            Env.push.webSubscriptionApi.unsubscribeByUserExceptSession(me, currentSessionId) inject
            Redirect(routes.Account.twoFactor)
        }
      }
    }
  }

  def disableTwoFactor = AuthBody { implicit ctx => me =>
    controllers.Auth.HasherRateLimit(me.username, ctx.req) { _ =>
      implicit val req = ctx.body
      Env.security.forms.disableTwoFactor(me) flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.twoFactor.disable(me, err))
        } { _ =>
          UserRepo.disableTwoFactor(me.id) inject Redirect(routes.Account.twoFactor)
        }
      }
    }
  }

  def close = Auth { implicit ctx => me =>
    Env.security.forms closeAccount me map { form =>
      Ok(html.account.close(me, form))
    }
  }

  def closeConfirm = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Env.security.forms closeAccount me flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.close(me, err))
      } { _ =>
        Env.current.closeAccount(me.id, self = true) inject {
          Redirect(routes.User show me.username) withCookies LilaCookie.newSession
        }
      }
    }
  }

  def kid = Auth { implicit ctx => me =>
    Ok(html.account.kid(me)).fuccess
  }
  def apiKid = Scoped(_.Preference.Read) { _ => me =>
    JsonOk(Json.obj("kid" -> me.kid)).fuccess
  }

  // App BC
  def kidToggle = Auth { ctx => me =>
    UserRepo.setKid(me, !me.kid) inject Ok
  }

  def kidPost = Auth { implicit ctx => me =>
    UserRepo.setKid(me, getBool("v")) inject Redirect(routes.Account.kid)
  }
  def apiKidPost = Scoped(_.Preference.Write) { req => me =>
    UserRepo.setKid(me, getBool("v", req)) inject jsonOkResult
  }

  private def currentSessionId(implicit ctx: Context) =
    ~Env.security.api.reqSessionId(ctx.req)

  def security = Auth { implicit ctx => me =>
    Env.security.api.dedup(me.id, ctx.req) >>
      Env.security.api.locatedOpenSessions(me.id, 50) map { sessions =>
        Ok(html.account.security(me, sessions, currentSessionId))
      }
  }

  def signout(sessionId: String) = Auth { implicit ctx => me =>
    if (sessionId == "all")
      lila.security.Store.closeUserExceptSessionId(me.id, currentSessionId) >>
        Env.push.webSubscriptionApi.unsubscribeByUserExceptSession(me, currentSessionId) inject
        Redirect(routes.Account.security)
    else
      lila.security.Store.closeUserAndSessionId(me.id, sessionId) >>
        Env.push.webSubscriptionApi.unsubscribeBySession(sessionId)
  }
}
