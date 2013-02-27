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
import ornicar.scalalib.Random

import core.CoreEnv

trait AuthImpl {

  protected def loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  protected def env: CoreEnv

  private def AccessUri = "access_uri"

  protected def logoutSucceeded(req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home)

  protected def authenticationFailed(implicit req: RequestHeader): Result =
    Redirect(routes.Auth.signup) withCookies LilaCookie.session(AccessUri, req.uri)

  protected def saveAuthentication(username: String)(implicit req: RequestHeader): String =
    (Random nextString 12) ~ { sessionId ⇒
      env.security.store.save(sessionId, username, req)
    }

  protected def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = saveAuthentication(username)
    val uri = req.session.get(AccessUri) | routes.Lobby.home.url
    Redirect(uri) withCookies LilaCookie.withSession { session ⇒
      session + ("sessionId" -> sessionId) - AccessUri
    }
  }

  protected def gotoSignupSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = saveAuthentication(username)
    Redirect(routes.User.show(username)) withCookies LilaCookie.session("sessionId", sessionId)
  }

  protected def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session get "sessionId" foreach env.security.store.delete
    logoutSucceeded(req) withCookies LilaCookie.newSession
  }

  protected def authorizationFailed(req: RequestHeader): Result =
    Forbidden("no permission")

  protected def authenticateUser(username: String, password: String): Option[User] =
    env.user.userRepo.authenticate(username, password).unsafePerformIO

  protected def restoreUser(req: RequestHeader): Option[User] = for {
    sessionId ← req.session.get("sessionId")
    if env.security.firewall accepts req
    username ← env.security.store.getUsername(sessionId)
    user ← (env.user.userRepo byId username).unsafePerformIO
  } yield user
}
