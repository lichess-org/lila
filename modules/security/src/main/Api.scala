package lila.security

import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import reactivemongo.api.ReadPreference
import reactivemongo.bson._

import lila.common.{ ApiVersion, IpAddress, EmailAddress }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class Api(
    coll: Coll,
    firewall: Firewall,
    geoIP: GeoIP,
    emailValidator: EmailAddressValidator
) {

  val AccessUri = "access_uri"

  def usernameForm = Form(single(
    "username" -> text
  ))

  def loginForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  private def loadedLoginForm(candidate: Option[User.LoginCandidate]) = Form(mapping(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  )(authenticateCandidate(candidate))(_.map(u => (u.username, "")))
    .verifying("invalidUsernameOrPassword", _.isDefined))

  def loadLoginForm(str: String): Fu[Form[Option[User]]] = {
    emailValidator.validate(EmailAddress(str)) match {
      case Some(email) => UserRepo.loginCandidateByEmail(email)
      case None if User.couldBeUsername(str) => UserRepo.loginCandidateById(User normalize str)
      case _ => fuccess(none)
    }
  } map loadedLoginForm _

  private def authenticateCandidate(candidate: Option[User.LoginCandidate])(username: String, password: String): Option[User] =
    candidate ?? { _(password) }

  def saveAuthentication(userId: String, apiVersion: Option[ApiVersion])(implicit req: RequestHeader): Fu[String] =
    UserRepo mustConfirmEmail userId flatMap {
      case true => fufail(Api MustConfirmEmail userId)
      case false =>
        val sessionId = Random secureString 12
        Store.save(sessionId, userId, req, apiVersion) inject sessionId
    }

  def restoreUser(req: RequestHeader): Fu[Option[FingerprintedUser]] =
    firewall accepts req flatMap {
      _ ?? {
        reqSessionId(req) ?? { sessionId =>
          Store userIdAndFingerprint sessionId flatMap {
            _ ?? { d =>
              if (d.isOld) Store.setDateToNow(sessionId)
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

  // def userIdsSharingFingerprint = userIdsSharingField("fp") _

  def recentByIpExists(ip: IpAddress): Fu[Boolean] = Store recentByIpExists ip

  private def userIdsSharingField(field: String)(userId: String): Fu[List[String]] =
    coll.distinctWithReadPreference[String, List](
      field,
      $doc("user" -> userId, field -> $doc("$exists" -> true)).some,
      readPreference = ReadPreference.secondaryPreferred
    ).flatMap {
      case Nil => fuccess(Nil)
      case values => coll.distinctWithReadPreference[String, List](
        "user",
        $doc(
          field $in values,
          "user" $ne userId
        ).some,
        ReadPreference.secondaryPreferred
      )
    }

  def recentUserIdsByFingerprint = recentUserIdsByField("fp") _

  def recentUserIdsByIp(ip: IpAddress) = recentUserIdsByField("ip")(ip.value)

  private def recentUserIdsByField(field: String)(value: String): Fu[List[String]] =
    coll.distinct[String, List](
      "user",
      $doc(
        field -> value,
        "date" $gt DateTime.now.minusYears(1)
      ).some
    )
}

object Api {

  case class MustConfirmEmail(userId: String) extends Exception
}
