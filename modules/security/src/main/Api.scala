package lila.security

import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.user.{ User, UserRepo }

final class Api(firewall: Firewall, tor: Tor, geoIP: GeoIP) {

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
        Store.save(sessionId, userId, req, apiVersion) inject sessionId
    }

  // blocking function, required by Play2 form
  private def authenticateUser(username: String, password: String): Option[User] =
    UserRepo.authenticate(username.toLowerCase, password) awaitSeconds 1

  def restoreUser(req: RequestHeader): Fu[Option[FingerprintedUser]] =
    firewall accepts req flatMap {
      _ ?? {
        reqSessionId(req) ?? { sessionId =>
          Store userIdAndFingerprint sessionId flatMap {
            _ ?? { d =>
              UserRepo.byId(d.user) map {
                _ map {
                  FingerprintedUser(_, d.fp.isDefined)
                }
              }
            }
          }
        }
      }
    }

  def locatedOpenSessions(userId: String, nb: Int): Fu[List[LocatedSession]] =
    Store.openSessions(userId, nb) map {
      _.map { session =>
        LocatedSession(session, geoIP(session.ip))
      }
    }

  def dedup(userId: String, req: RequestHeader): Funit =
    reqSessionId(req) ?? { Store.dedup(userId, _) }

  def setFingerprint(req: RequestHeader, fingerprint: String): Funit =
    reqSessionId(req) ?? { Store.setFingerprint(_, fingerprint) }

  def reqSessionId(req: RequestHeader) = req.session get "sessionId"

  def userIdsSharingIp = userIdsSharingField("ip") _

  def userIdsSharingFingerprint = userIdsSharingField("fp") _

  private def userIdsSharingField(field: String)(userId: String): Fu[List[String]] =
    tube.storeColl.find(
      BSONDocument("user" -> userId, field -> BSONDocument("$exists" -> true)),
      BSONDocument(field -> true)
    ).cursor[BSONDocument]().collect[List]().map {
        _.flatMap(_.getAs[String](field))
      }.flatMap {
        case Nil => fuccess(Nil)
        case values => tube.storeColl.find(
          BSONDocument(
            field -> BSONDocument("$in" -> values.distinct),
            "user" -> BSONDocument("$ne" -> userId)
          ),
          BSONDocument("user" -> true)
        ).cursor[BSONDocument]().collect[List]().map {
            _.flatMap(_.getAs[String]("user"))
          }
      }

  def userIdsByIp(ip: String): Fu[List[String]] =
    tube.storeColl.find(
      BSONDocument("ip" -> ip),
      BSONDocument("user" -> true)
    ).cursor[BSONDocument]().collect[List]().map {
        _.flatMap(_.getAs[String]("user"))
      }.map(_.distinct)
}

object Api {

  case object AuthFromTorExitNode extends Exception
  case class MustConfirmEmail(userId: String) extends Exception
}
