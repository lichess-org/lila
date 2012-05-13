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

trait AuthConfigImpl extends AuthConfig {

  val loginForm = Form(mapping(
    "username" -> text,
    "password" -> text
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", result ⇒ result.isDefined)
  )

  def env: SystemEnv

  type Id = String

  type User = model.User

  type Authority = Permission

  val idManifest: ClassManifest[Id] = classManifest[Id]

  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id): Option[User] = (env.userRepo byUsername id).unsafePerformIO

  def logoutSucceeded[A](request: Request[A]): PlainResult = 
    Redirect(routes.Lobby.home)

  def authenticationFailed[A](request: Request[A]): PlainResult = 
    Redirect(routes.Lobby.home).withSession("access_uri" -> request.uri)

  def loginSucceeded[A](request: Request[A]): PlainResult = {
    val uri = request.session.get("access_uri").getOrElse(routes.Lobby.home.url)
    request.session - "access_uri"
    Redirect(uri)
  }

  def authorizationFailed[A](request: Request[A]): PlainResult = 
    Forbidden("no permission")

  def authorize(user: User, authority: Authority): Boolean = 
    Permission(user.roles) contains authority

  def authenticateUser(username: String, password: String): Option[User] =
    env.userRepo.authenticate(username, password).unsafePerformIO
}
