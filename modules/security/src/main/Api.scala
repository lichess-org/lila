package lila.security

import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader

import lila.user.{ User, UserRepo }

private[security] final class Api(firewall: Firewall) {

  val AccessUri = "access_uri"

  def loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u => (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  def saveAuthentication(username: String)(implicit req: RequestHeader): Fu[String] = {
    val sessionId = Random nextStringUppercase 12
    Store.save(sessionId, username.toLowerCase, req) inject sessionId
  }

  // blocking function, required by Play2 form
  def authenticateUser(username: String, password: String): Option[User] =
    UserRepo.authenticate(username.toLowerCase, password).await

  def restoreUser(req: RequestHeader): Fu[Option[User]] =
    firewall accepts req flatMap {
      _ ?? {
        req.session.get("sessionId") ?? { sessionId =>
          Store userId sessionId flatMap { _ ?? UserRepo.byId }
        }
      }
    }

  def userIdsSharingIp(userId: String) = {
    import tube.storeTube
    import lila.db.api._
    import play.api.libs.json._
    $primitive(Json.obj("user" -> userId), "ip")(_.asOpt[String]) flatMap { ips =>
      $primitive(Json.obj("ip" -> $in(ips), "user" -> $ne(userId)), "user")(_.asOpt[String])
    }
  }
}
