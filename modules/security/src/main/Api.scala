package lila.security

import lila.db.ByteArray
import lila.db.ByteArray.ByteArrayBSONHandler
import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.user.{ User, UserRepo }

private[security] final class Api(firewall: Firewall, tor: Tor) {

  val AccessUri = "access_uri"

  def loginForm = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateUser)(_.map(u => (u.username, "")))
    .verifying("Invalid username or password", _.isDefined)
  )

  def saveAuthentication(userId: String, apiVersion: Option[Int])(implicit req: RequestHeader): Fu[String] =
    if (tor isExitNode req.remoteAddress) fufail(Api.AuthFromTorExitNode)
    else UserRepo mustConfirmEmail userId flatMap {
      case true => fufail(Api MustConfirmEmail userId)
      case false =>
        val sessionId = Random nextStringUppercase 12
        Store.save(
          sessionId, userId, req, apiVersion, tor isExitNode req.remoteAddress
        ) inject sessionId
    }

  // blocking function, required by Play2 form
  private def authenticateUser(username: String, password: String): Option[User] =
    UserRepo.authenticate(username.toLowerCase, password).await

  def restoreUser(req: RequestHeader): Fu[Option[User]] =
    firewall accepts req flatMap {
      _ ?? {
        reqSessionId(req) ?? { sessionId =>
          Store userId sessionId flatMap { _ ?? UserRepo.byId }
        }
      }
    }

  def setFingerprint(req: RequestHeader, fingerprint: String): Funit =
    reqSessionId(req) ?? { Store.setFingerprint(_, fingerprint) }

  private def reqSessionId(req: RequestHeader) = req.session get "sessionId"

  def userIdsSharingIp(userId: String): Fu[List[String]] =
    tube.storeColl.find(
      BSONDocument("user" -> userId),
      BSONDocument("ip" -> true)
    ).cursor[BSONDocument]().collect[List]().map {
        _.flatMap(_.getAs[String]("ip"))
      }.flatMap { ips =>
        tube.storeColl.find(
          BSONDocument(
            "ip" -> BSONDocument("$in" -> ips.distinct),
            "user" -> BSONDocument("$ne" -> userId)
          ),
          BSONDocument("user" -> true)
        ).cursor[BSONDocument]().collect[List]().map {
            _.flatMap(_.getAs[String]("user"))
          }
      }

  def userIdsSharingFingerprint(userId: String): Fu[List[String]] =
    tube.storeColl.find(
      BSONDocument("user" -> userId, "fp" -> BSONDocument("$exists" -> true)),
      BSONDocument("fp" -> true)
    ).cursor[BSONDocument]().collect[List]().map {
        _.flatMap(_.getAs[ByteArray]("fp"))
      }.flatMap {
        case Nil => fuccess(Nil)
        case fps => tube.storeColl.find(
          BSONDocument(
            "fp" -> BSONDocument("$in" -> fps.distinct),
            "user" -> BSONDocument("$ne" -> userId)
          ),
          BSONDocument("user" -> true)
        ).cursor[BSONDocument]().collect[List]().map {
            _.flatMap(_.getAs[String]("user"))
          }
      }
}

object Api {

  case object AuthFromTorExitNode extends Exception
  case class MustConfirmEmail(userId: String) extends Exception
}
