package lila
package security

import controllers.routes
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.{Request, PlainResult, Controller}
import play.api.data._
import play.api.data.Forms._
import jp.t2v.lab.play20.auth._
import core.CoreEnv

trait AuthConfigImpl extends AuthConfig {

  val loginForm = Form(mapping(
    "username" -> text,
    "password" -> text
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", result ⇒ result.isDefined)
  )

  def env: CoreEnv

  type Id = String

  type User = user.User

  type Authority = Permission

  val idManifest: ClassManifest[Id] = classManifest[Id]

  val sessionTimeoutInSeconds: Int = 3600 * 24 * 30

  def resolveUser(id: Id): Option[User] = 
    (env.user.userRepo byUsername id).unsafePerformIO

  def logoutSucceeded[A](req: Request[A]): PlainResult = 
    Redirect(routes.Lobby.home)

  def authenticationFailed(req: RequestHeader): PlainResult = 
    Redirect(routes.Lobby.home).withSession("access_uri" -> req.uri)

  def authenticationFailed[A](req: Request[A]): PlainResult = 
    authenticationFailed(req)

  def loginSucceeded[A](req: Request[A]): PlainResult = {
    val uri = req.session.get("access_uri").getOrElse(routes.Lobby.home.url)
    req.session - "access_uri"
    Redirect(uri)
  }

  def authorizationFailed(req: RequestHeader): PlainResult = 
    Forbidden("no permission")

  def authorizationFailed[A](req: Request[A]): PlainResult = 
    authorizationFailed(req)

  def authorize(user: User, authority: Authority): Boolean = 
    Permission(user.roles) contains authority

  def authenticateUser(username: String, password: String): Option[User] =
    env.user.userRepo.authenticate(username, password).unsafePerformIO
}
