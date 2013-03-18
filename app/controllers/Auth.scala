package controllers

import lila.app._
import lila.api._
import lila.http.LilaCookie

import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

object Auth extends LilaController {

  // protected def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) = {
  //   val sessionId = saveAuthentication(username)
  //   val uri = req.session.get(AccessUri) | routes.Lobby.home.url
  //   Redirect(uri) withCookies LilaCookie.withSession { session ⇒
  //     session + ("sessionId" -> sessionId) - AccessUri
  //   }
  // }

  // protected def gotoSignupSucceeded[A](username: String)(implicit req: RequestHeader) = {
  //   val sessionId = saveAuthentication(username)
  //   Redirect(routes.User.show(username)) withCookies LilaCookie.session("sessionId", sessionId)
  // }

  protected def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session get "sessionId" foreach env.security.store.delete
    logoutSucceeded(req) withCookies LilaCookie.newSession
  }

  protected def logoutSucceeded(req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home)

  // protected def authenticationFailed(implicit req: RequestHeader): Result =
  //   Redirect(routes.Auth.signup) withCookies LilaCookie.session(AccessUri, req.uri)
}
