package lila
package security

import controllers.routes
import user.User

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.{ Request, PlainResult, Controller }
import play.api.data._
import play.api.data.Forms._
import ornicar.scalalib.OrnicarRandom

import core.CoreEnv

trait AuthImpl {

  val loginForm = Form(mapping(
    "username" -> text,
    "password" -> text
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", result ⇒ result.isDefined)
  )

  def env: CoreEnv

  def logoutSucceeded(req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home)

  def authenticationFailed(req: RequestHeader): PlainResult =
    Redirect(routes.Lobby.home).withSession("access_uri" -> req.uri)

  def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = OrnicarRandom nextAsciiString 16
    env.securityStore.save(sessionId, username, req)
    loginSucceeded(req).withSession("sessionId" -> sessionId)
  }

  def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session.get("sessionId") foreach env.securityStore.delete
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
    username ← env.securityStore.getUsername(sessionId)
    user ← (env.user.userRepo byUsername username).unsafePerformIO
  } yield user
}
