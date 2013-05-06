package lila.security

import lila.user.{ User, UserRepo }

import play.api.mvc.RequestHeader
import play.api.data._
import play.api.data.Forms._
import ornicar.scalalib.Random

private[security] final class Api(firewall: Firewall) {

  val AccessUri = "access_uri"

  def loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u ⇒ (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  def saveAuthentication(username: String)(implicit req: RequestHeader): Fu[String] = {
    val sessionId = Random nextString 12 
    Store.save(sessionId, username, req) inject sessionId
  }

  // blocking function, required by Play2 form
  def authenticateUser(username: String, password: String): Option[User] =
    UserRepo.authenticate(username, password).await

  def restoreUser(req: RequestHeader): Fu[Option[User]] =
    firewall accepts req flatMap { accepted ⇒
      ~accepted.option(
        req.session.get("sessionId").fold(fuccess(none[User])) { sessionId ⇒
          Store getUsername sessionId flatMap { username ⇒
            username.zmap(UserRepo.named)
          }
        }
      )
    }
}
