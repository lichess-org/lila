package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import lila.user.{ User => UserModel, UserRepo, TotpSecret }
import UserModel.ClearPassword
import views.html

object Account extends LilaController {

  private def env = Env.user
  private def relationEnv = Env.relation

  def profile = Auth { implicit ctx => me =>
    Ok(html.account.profile(me, env.forms profileOf me)).fuccess
  }

  def profileApply = AuthBody { implicit ctx => me =>
    implicit val req: Request[_] = ctx.body
    FormFuResult(env.forms.profile) { err =>
      fuccess(html.account.profile(me, err))
    } { profile =>
      UserRepo.setProfile(me.id, profile) inject Redirect(routes.User show me.username)
    }
  }

  def info = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => relationEnv.api.countFollowers(me.id) zip
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
    )
  }

  def nowPlaying = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => lila.game.GameRepo.urgentGames(me) map { povs =>
        val nb = getInt("nb") | 9
        Ok(Json.obj("nowPlaying" -> JsArray(povs take nb map Env.api.lobbyApi.nowPlaying)))
      }
    )
  }

  def apiMe = Scoped() { _ => me =>
    Env.api.userApi.extended(me, me.some) map { json =>
      Ok(json) as JSON
    }
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
    implicit val req = ctx.body
    env.forms passwd me flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.passwd(err))
      } { data =>
        controllers.Auth.HasherRateLimit(me.username, req) { _ =>
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
        Ok(Json.obj("email" -> email.value))
      }
    }
  }

  def renderCheckYourEmail(implicit ctx: Context) =
    html.auth.checkYourEmail(lila.security.EmailConfirm.cookie get ctx.req)

  def emailApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    emailForm(me) flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.email(me, err))
      } { data =>
        val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, data.realEmail)
        controllers.Auth.EmailConfirmRateLimit(newUserEmail, ctx.req) {
          Env.security.emailChange.send(me, newUserEmail.email) inject Redirect {
            s"${routes.Account.email}?check=1"
          }
        }
      }
    }
  }

  def emailConfirm(token: String) = Open { implicit ctx =>
    Env.security.emailChange.confirm(token) flatMap {
      _ ?? { user =>
        controllers.Auth.authenticateUser(user, result = Redirect {
          s"${routes.Account.email}?ok=1"
        }.fuccess.some)
      }
    }
  }

  def twoFactor = Auth { implicit ctx => me =>
    if (me.totpSecret.isDefined)
      Env.security.forms.disableTwoFactor(me) map { form =>
        html.account.disableTwoFactor(me, form)
      }
    else
      Env.security.forms.setupTwoFactor(me) map { form =>
        html.account.setupTwoFactor(me, form)
      }
  }

  def setupTwoFactor = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Env.security.forms.setupTwoFactor(me) flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.setupTwoFactor(me, err))
      } { data =>
        UserRepo.setupTwoFactor(me.id, TotpSecret(data.secret)) inject
          Redirect(routes.Account.twoFactor)
      }
    }
  }

  def disableTwoFactor = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Env.security.forms.disableTwoFactor(me) flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.disableTwoFactor(me, err))
      } { _ =>
        UserRepo.disableTwoFactor(me.id) inject Redirect(routes.Account.twoFactor)
      }
    }
  }

  def close = Auth { implicit ctx => me =>
    Ok(html.account.close(me, Env.security.forms.closeAccount)).fuccess
  }

  def closeConfirm = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    FormFuResult(Env.security.forms.closeAccount) { err =>
      fuccess(html.account.close(me, err))
    } { password =>
      Env.user.authenticator.authenticateById(me.id, ClearPassword(password)).map(_.isDefined) flatMap {
        case false => BadRequest(html.account.close(me, Env.security.forms.closeAccount)).fuccess
        case true => Env.current.closeAccount(me.id, self = true) inject {
          Redirect(routes.User show me.username) withCookies LilaCookie.newSession
        }
      }
    }
  }

  def kid = Auth { implicit ctx => me =>
    Ok(html.account.kid(me)).fuccess
  }
  def apiKid = Scoped(_.Preference.Read) { _ => me =>
    Ok(Json.obj("kid" -> me.kid)).fuccess
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
      lila.security.Store.closeUserExceptSessionId(me.id, currentSessionId) inject
        Redirect(routes.Account.security)
    else
      lila.security.Store.closeUserAndSessionId(me.id, sessionId)
  }
}
