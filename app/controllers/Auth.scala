package controllers

import play.api.data._, Forms._
import play.api.libs.json.Json
import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.user.UserRepo
import views._

object Auth extends LilaController {

  private def api = Env.security.api
  private def forms = Env.security.forms

  def login = Open { implicit ctx =>
    val referrer = get("referrer")
    Ok(html.auth.login(api.loginForm, referrer)) fuccess
  }

  def authenticate = OpenBody { implicit ctx =>
    val referrer = get("referrer")
    Firewall {
      implicit val req = ctx.body
      api.loginForm.bindFromRequest.fold(
        err => negotiate(
          html = Unauthorized(html.auth.login(err, referrer)).fuccess,
          api = _ => Unauthorized(err.errorsAsJson).fuccess
        ),
        _.fold(InternalServerError("authenticate error").fuccess) { u =>
          u.ipBan.fold(
            Env.security.firewall.blockIp(req.remoteAddress) inject BadRequest("blocked by firewall"),
            api saveAuthentication u.id flatMap { sessionId =>
              negotiate(
                html = Redirect {
                  referrer.filter(_.nonEmpty) orElse req.session.get(api.AccessUri) getOrElse routes.Lobby.home.url
                }.fuccess,
                api = _ => Ok(Env.user.jsonView(u, extended = true)).fuccess
              ) map {
                  _ withCookies LilaCookie.withSession { session =>
                    session + ("sessionId" -> sessionId) - api.AccessUri
                  }
                }
            }
          )
        }
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
    forms.signupWithCaptcha map {
      case (form, captcha) => Ok(html.auth.signup(form, captcha))
    }
  }

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.signup.bindFromRequest.fold(
      err => forms.anyCaptcha map { captcha =>
        BadRequest(html.auth.signup(err, captcha))
      },
      data => Firewall {
        UserRepo.create(data.username, data.password, ctx.blindMode) flatMap { userOption =>
          val user = userOption err "No user could be created for %s".format(data.username)
          api saveAuthentication user.id map { sessionId =>
            Redirect(routes.User.show(user.username)) withCookies LilaCookie.session("sessionId", sessionId)
          }
        }
      }
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
        BadRequest(html.auth.passwordReset(err, captcha))
      },
      data => UserRepo byEmail data.email flatten s"No such user: ${data.email}" flatMap { user =>
        Env.security.passwordReset(user, data.email) inject Redirect(routes.Auth.passwordResetSent(data.email))
      }
    )
  }

  def passwordResetSent(email: String) = Open { implicit ctx =>
    fuccess {
      Ok(html.auth.passwordResetSent(email))
    }
  }
}
