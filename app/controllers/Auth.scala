package controllers

import play.api.data._, Forms._
import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.user.{ UserRepo, HistoryRepo }
import views._

object Auth extends LilaController {

  private def api = Env.security.api
  private def forms = Env.security.forms

  private def gotoLoginSucceeded[A](username: String, referrer: Option[String])(implicit req: RequestHeader) =
    api saveAuthentication username map { sessionId =>
      val uri = referrer.filter(_.nonEmpty) orElse req.session.get(api.AccessUri) getOrElse routes.Lobby.home.url
      Redirect(uri) withCookies LilaCookie.withSession { session =>
        session + ("sessionId" -> sessionId) - api.AccessUri
      }
    }

  private def gotoSignupSucceeded[A](username: String)(implicit req: RequestHeader) =
    api saveAuthentication username map { sessionId =>
      Redirect(routes.User.show(username)) withCookies LilaCookie.session("sessionId", sessionId)
    }

  def login = Open { implicit ctx =>
    val referrer = get("referrer")
    Ok(html.auth.login(api.loginForm, referrer)) fuccess
  }

  def authenticate = OpenBody { implicit ctx =>
    val referrer = get("referrer")
    Firewall {
      implicit val req = ctx.body
      api.loginForm.bindFromRequest.fold(
        err => BadRequest(html.auth.login(err, referrer)).fuccess,
        _.fold(InternalServerError("authenticate error").fuccess) { u =>
          u.ipBan.fold(
            Env.security.firewall.blockIp(req.remoteAddress) inject BadRequest("blocked by firewall"),
            gotoLoginSucceeded(u.username, referrer)
          )
        }
      )
    }
  }

  def logout = Open { implicit ctx =>
    gotoLogoutSucceeded(ctx.req) fuccess
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
          HistoryRepo.create(user) >> gotoSignupSucceeded(user.username)
        }
      }
    )
  }

  def newPassword = AuthBody { implicit ctx =>
    me =>
      if (!me.artificial) fuccess(Redirect(routes.Lobby.home))
      else {
        implicit val req = ctx.body
        forms.newPassword.bindFromRequest.fold(
          err => fuccess {
            BadRequest(html.auth.artificialPassword(me, err))
          },
          pass => UserRepo.artificialSetPassword(me.id, pass) map { _ =>
            Redirect(routes.Lobby.home)
          }
        )
      }
  }

  private def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session get "sessionId" foreach lila.security.Store.delete
    logoutSucceeded(req) withCookies LilaCookie.newSession
  }

  private def logoutSucceeded(req: RequestHeader): Result =
    Redirect(routes.Lobby.home)
}
