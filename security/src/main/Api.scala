package lila.security

import lila.user.{ User, UserRepo }

import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import ornicar.scalalib.Random

final class Api(firewall: Firewall) {

  def AccessUri = "access_uri"

  def loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  def saveAuthentication(username: String)(implicit req: RequestHeader): String =
    (Random nextString 12) ~ { sessionId ⇒ Store.save(sessionId, username, req) }

  def authorizationFailed(req: RequestHeader): Result =
    Forbidden("no permission")

  // blocking function, required by Play2 form
  def authenticateUser(username: String, password: String): Option[User] =
    UserRepo.authenticate(username, password).await

  def restoreUser(req: RequestHeader): Fu[Option[User]] =
    ~(firewall accepts req).option(
      req.session.get("sessionId").fold(fuccess(none[User])) { sessionId ⇒
        Store getUsername sessionId flatMap { username ⇒
          username.zmap(UserRepo.named)
        }
      }
    )
}
