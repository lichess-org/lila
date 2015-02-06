package controllers

import play.api.data._, Forms._
import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.user.{ UserRepo, User => UserModel }
import views._

object Auth extends LilaController {

  private def api = Env.security.api
  private def forms = Env.security.forms

  private def mobileUserOk(u: UserModel): Fu[Result] =
    lila.game.GameRepo nowPlaying u map { povs =>
      Ok {
        Env.user.jsonView(u, extended = true) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 9 map Env.api.lobbyApi.nowPlaying))
      }
    }

  private def authenticateUser(u: UserModel)(implicit ctx: lila.api.Context) = {
    implicit val req = ctx.req
    u.ipBan.fold(
      Env.security.firewall.blockIp(req.remoteAddress) inject BadRequest("blocked by firewall"),
      api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
        negotiate(
          html = Redirect {
            get("referrer").filter(_.nonEmpty) orElse req.session.get(api.AccessUri) getOrElse routes.Lobby.home.url
          }.fuccess,
          api = _ => mobileUserOk(u)
        ) map {
            _ withCookies LilaCookie.withSession { session =>
              session + ("sessionId" -> sessionId) - api.AccessUri
            }
          }
      }
    )
  }

  def login = Open { implicit ctx =>
    val referrer = get("referrer")
    Ok(html.auth.login(api.loginForm, referrer)).fuccess
  }

  def authenticate = OpenBody { implicit ctx =>
    Firewall {
      implicit val req = ctx.body
      api.loginForm.bindFromRequest.fold(
        err => negotiate(
          html = Unauthorized(html.auth.login(err, get("referrer"))).fuccess,
          api = _ => Unauthorized(err.errorsAsJson).fuccess
        ),
        _.fold(InternalServerError("authenticate error").fuccess)(authenticateUser)
      )
    }
  }

  def logout = Open { implicit ctx =>
    implicit val req = ctx.req
    req.session get "sessionId" foreach lila.security.Store.delete
    negotiate(
      html = fuccess(Redirect(routes.Lobby.home)),
      api = apiVersion => Ok(Json.obj("ok" -> true)).fuccess
    ) map (_ withCookies LilaCookie.newSession)
  }

  def signup = Open { implicit ctx =>
    forms.signup.websiteWithCaptcha map {
      case (form, captcha) => Ok(html.auth.signup(form, captcha))
    }
  }

  private def doSignup(username: String, password: String)(res: UserModel => Fu[Result])(implicit ctx: lila.api.Context) =
    Firewall {
      implicit val req = ctx.req
      UserRepo.create(username, password, ctx.blindMode, ctx.mobileApiVersion) flatMap { userOption =>
        val user = userOption err "No user could be created for %s".format(username)
        api.saveAuthentication(
          user.id,
          ctx.mobileApiVersion
        ) flatMap { sessionId =>
            res(user) map {
              _ withCookies LilaCookie.session("sessionId", sessionId)
            }
          }
      }
    }

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    negotiate(
      html = forms.signup.website.bindFromRequest.fold(
        err => forms.anyCaptcha map { captcha =>
          BadRequest(html.auth.signup(err, captcha))
        },
        data => doSignup(data.username, data.password) { user =>
          fuccess(Redirect(routes.User.show(user.username)))
        }
      ),
      api = _ => forms.signup.mobile.bindFromRequest.fold(
        err => fuccess(BadRequest(Json.obj(
          "error" -> err.errorsAsJson
        ))),
        data => doSignup(data.username, data.password)(mobileUserOk)
      )
    )
  }

  def passwordReset = Open { implicit ctx =>
    forms.passwordResetWithCaptcha map {
      case (form, captcha) => Ok(html.auth.passwordReset(form, captcha))
    }
  }

  def passwordResetApply = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.passwordReset.bindFromRequest.fold(
      err => forms.anyCaptcha map { captcha =>
        BadRequest(html.auth.passwordReset(err, captcha, false.some))
      },
      data => UserRepo enabledByEmail data.email flatMap {
        case Some(user) =>
          Env.security.passwordReset.send(user, data.email) inject Redirect(routes.Auth.passwordResetSent(data.email))
        case None => forms.passwordResetWithCaptcha map {
          case (form, captcha) => BadRequest(html.auth.passwordReset(form, captcha, false.some))
        }
      }
    )
  }

  def passwordResetSent(email: String) = Open { implicit ctx =>
    fuccess {
      Ok(html.auth.passwordResetSent(email))
    }
  }

  def passwordResetConfirm(token: String) = Open { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      case Some(user) =>
        fuccess(html.auth.passwordResetConfirm(user, token, forms.passwdReset, none))
      case _ => notFound
    }
  }

  def passwordResetConfirmApply(token: String) = OpenBody { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      case Some(user) =>
        implicit val req = ctx.body
        FormFuResult(forms.passwdReset) { err =>
          fuccess(html.auth.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          UserRepo.passwd(user.id, data.newPasswd1) >> authenticateUser(user)
        }
      case _ => notFound
    }
  }
}
