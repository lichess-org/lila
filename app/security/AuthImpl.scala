package lila
package security

import controllers.routes
import user.User
import http.LilaCookie

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.{ Request, PlainResult, Controller }
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints
import ornicar.scalalib.OrnicarRandom

import core.CoreEnv

trait AuthImpl {

  val loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  def env: CoreEnv

  def logoutSucceeded(req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home)

  def authenticationFailed(implicit req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home) withCookies LilaCookie("access_uri", req.uri)

  def saveAuthentication(username: String)(implicit req: RequestHeader): String =
    (OrnicarRandom nextAsciiString 12) ~ { sessionId ⇒
      env.security.store.save(sessionId, username, req)
    }

  def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = saveAuthentication(username)
    loginSucceeded(req) withCookies LilaCookie("sessionId", sessionId)
  }

  def gotoSignupSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = saveAuthentication(username)
    Redirect(routes.User.show(username)) withCookies LilaCookie("sessionId", sessionId)
  }

  def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session.get("sessionId") foreach env.security.store.delete
    logoutSucceeded(req).withNewSession
  }

  def loginSucceeded(req: RequestHeader): PlainResult = {
    val uri = req.session.get("access_uri").getOrElse(routes.Lobby.home.url)
    req.session - "access_uri"
    Redirect(uri)
  }

  def authorizationFailed(req: RequestHeader): PlainResult =
    Forbidden("no permission")

  def authenticateUser(username: String, password: String): Option[User] =
    env.user.userRepo.authenticate(username, password).unsafePerformIO

  def restoreUser(req: RequestHeader): Option[User] = for {
    sessionId ← req.session.get("sessionId")
    username ← env.security.store.getUsername(sessionId)
    user ← (env.user.userRepo byId username).unsafePerformIO
  } yield user
}
