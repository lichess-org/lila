package controllers

import play.api.mvc._

import controllers.Auth.HasherRateLimit
import lila.api.Context
import lila.app._
import lila.common.PimpedJson._
import lila.common.{ LilaCookie, EmailAddress }
import lila.user.{ User => UserModel, UserRepo }
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

  def info = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => ctx.me match {
        case None => fuccess(unauthorizedApiResult)
        case Some(me) =>
          relationEnv.api.countFollowers(me.id) zip
            relationEnv.api.countFollowing(me.id) zip
            Env.pref.api.getPref(me) zip
            lila.game.GameRepo.urgentGames(me) zip
            Env.challenge.api.countInFor.get(me.id) map {
              case nbFollowers ~ nbFollowing ~ prefs ~ povs ~ nbChallenges =>
                Env.current.bus.publish(lila.user.User.Active(me), 'userActive)
                Ok {
                  import play.api.libs.json._
                  import lila.pref.JsonView._
                  Env.user.jsonView(me) ++ Json.obj(
                    "prefs" -> prefs,
                    "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying),
                    "nbFollowing" -> nbFollowing,
                    "nbFollowers" -> nbFollowers,
                    "nbChallenges" -> nbChallenges
                  ).add("kid" -> me.kid)
                    .add("troll" -> me.troll)
                }
            }
      }
    ) map ensureSessionId(ctx.req)
  }

  def dasher = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => ctx.me match {
        case None => fuccess(unauthorizedApiResult)
        case Some(me) => Env.pref.api.getPref(me) map { prefs =>
          Ok {
            import play.api.libs.json._
            import lila.pref.JsonView._
            lila.common.LightUser.lightUserWrites.writes(me.light) ++ Json.obj(
              "coach" -> isGranted(_.Coach),
              "prefs" -> prefs
            )
          }
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
        HasherRateLimit(me.username) { _ =>
          Env.user.authenticator.setPassword(me.id, data.newPasswd1) inject
            Redirect(s"${routes.Account.passwd}?ok=1")
        }
      }
    }
  }

  private def emailForm(user: UserModel) = UserRepo email user.id flatMap {
    Env.security.forms.changeEmail(user, _)
  }

  def email = Auth { implicit ctx => me =>
    if (getBool("check")) Ok(html.auth.checkYourEmail(me)).fuccess
    else emailForm(me) map { form =>
      Ok(html.account.email(me, form))
    }
  }

  def emailApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    emailForm(me) flatMap { form =>
      FormFuResult(form) { err =>
        fuccess(html.account.email(me, err))
      } { data =>
        Env.security.emailChange.send(me, data.realEmail) inject Redirect {
          s"${routes.Account.email}?check=1"
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

  def close = Auth { implicit ctx => me =>
    Ok(html.account.close(me, Env.security.forms.closeAccount)).fuccess
  }

  def closeConfirm = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    FormFuResult(Env.security.forms.closeAccount) { err =>
      fuccess(html.account.close(me, err))
    } { password =>
      Env.user.authenticator.authenticateById(me.id, password).map(_.isDefined) flatMap {
        case false => BadRequest(html.account.close(me, Env.security.forms.closeAccount)).fuccess
        case true => doClose(me) inject {
          Redirect(routes.User show me.username) withCookies LilaCookie.newSession
        }
      }
    }
  }

  private[controllers] def doClose(user: UserModel) =
    (UserRepo disable user) >>-
      env.onlineUserIdMemo.remove(user.id) >>
      relationEnv.api.unfollowAll(user.id) >>
      Env.user.rankingApi.remove(user.id) >>
      Env.team.api.quitAll(user.id) >>-
      Env.challenge.api.removeByUserId(user.id) >>-
      Env.tournament.api.withdrawAll(user) >>
      Env.plan.api.cancel(user).nevermind >>
      (Env.security.store disconnect user.id)

  def kid = Auth { implicit ctx => me =>
    Ok(html.account.kid(me)).fuccess
  }

  def kidConfirm = Auth { ctx => me =>
    (UserRepo toggleKid me) inject Redirect(routes.Account.kid)
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
