package lila.security

import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.user.{ User, UserRepo }

final class Api(
    coll: Coll,
    firewall: Firewall,
    tor: Tor,
    geoIP: GeoIP,
    emailAddress: EmailAddress) {

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
  private def authenticateUser(usernameOrEmail: String, password: String): Option[User] =
    (emailAddress.validate(usernameOrEmail) match {
      case Some(email) => UserRepo.authenticateByEmail(email, password)
      case None        => UserRepo.authenticateById(User normalize usernameOrEmail, password)
    }) awaitSeconds 2

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

  def setFingerprint(req: RequestHeader, fingerprint: String): Fu[Option[String]] =
    reqSessionId(req) ?? { Store.setFingerprint(_, fingerprint) map some }

  def reqSessionId(req: RequestHeader) = req.session get "sessionId"

  def userIdsSharingIp = userIdsSharingField("ip") _

  def userIdsSharingFingerprint = userIdsSharingField("fp") _

  private def userIdsSharingField(field: String)(userId: String): Fu[List[String]] =
    coll.distinct(
      field,
      BSONDocument("user" -> userId, field -> BSONDocument("$exists" -> true)).some
    ).flatMap {
        case Nil => fuccess(Nil)
        case values => coll.distinct(
          "user",
          BSONDocument(
            field -> BSONDocument("$in" -> values),
            "user" -> BSONDocument("$ne" -> userId)
          ).some
        ) map lila.db.BSON.asStrings
      }

  def recentUserIdsByFingerprint = recentUserIdsByField("fp") _

  def recentUserIdsByIp = recentUserIdsByField("ip") _

  private def recentUserIdsByField(field: String)(value: String): Fu[List[String]] =
    coll.distinct(
      "user",
      BSONDocument(
        field -> value,
        "date" -> BSONDocument("$gt" -> DateTime.now.minusYears(1))
      ).some
    ) map lila.db.BSON.asStrings
}

object Api {

  case object AuthFromTorExitNode extends Exception
  case class MustConfirmEmail(userId: String) extends Exception
}
