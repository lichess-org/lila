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

final class SecurityApi(
    coll: Coll,
    firewall: Firewall,
    geoIP: GeoIP,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator
) {

  val AccessUri = "access_uri"

  lazy val usernameForm = Form(single(
    "username" -> text
  ))

  lazy val loginForm = Form(tuple(
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
      case Some(email) => authenticator.loginCandidateByEmail(email)
      case None if User.couldBeUsername(str) => authenticator.loginCandidateById(User normalize str)
      case _ => fuccess(none)
    }
  } map loadedLoginForm _

  private def authenticateCandidate(candidate: Option[User.LoginCandidate])(username: String, password: String): Option[User] =
    candidate ?? { _(User.ClearPassword(password)) }

  def saveAuthentication(userId: User.ID, apiVersion: Option[ApiVersion])(implicit req: RequestHeader): Fu[String] =
    UserRepo mustConfirmEmail userId flatMap {
      case true => fufail(SecurityApi MustConfirmEmail userId)
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

  def locatedOpenSessions(userId: User.ID, nb: Int): Fu[List[LocatedSession]] =
    Store.openSessions(userId, nb) map {
      _.map { session =>
        LocatedSession(session, geoIP(session.ip))
      }
    }

  def dedup(userId: User.ID, req: RequestHeader): Funit =
    reqSessionId(req) ?? { Store.dedup(userId, _) }

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req) ?? { Store.setFingerPrint(_, fp) map some }

  def reqSessionId(req: RequestHeader) = req.session get "sessionId"

  def userIdsSharingIp = userIdsSharingField("ip") _

  def recentByIpExists(ip: IpAddress): Fu[Boolean] = Store recentByIpExists ip

  def recentByPrintExists(fp: FingerPrint): Fu[Boolean] = Store recentByPrintExists fp

  private def userIdsSharingField(field: String)(userId: String): Fu[List[User.ID]] =
    coll.distinctWithReadPreference[User.ID, List](
      field,
      $doc("user" -> userId, field $exists true).some,
      readPreference = ReadPreference.secondaryPreferred
    ).flatMap {
        case Nil => fuccess(Nil)
        case values => coll.distinctWithReadPreference[User.ID, List](
          "user",
          $doc(
            field $in values,
            "user" $ne userId
          ).some,
          ReadPreference.secondaryPreferred
        )
      }

  def recentUserIdsByFingerHash(fh: FingerHash) = recentUserIdsByField("fp")(fh.value)

  def recentUserIdsByIp(ip: IpAddress) = recentUserIdsByField("ip")(ip.value)

  def shareIpOrPrint(u1: User.ID, u2: User.ID): Fu[Boolean] =
    Store.ipsAndFps(List(u1, u2), max = 100) map {
      _.foldLeft(Set.empty[String] -> false) {
        case ((u1s, true), _) => u1s -> true
        case ((u1s, _), entry) if u1 == entry.user =>
          val newU1s = u1s + entry.ip.value
          entry.fp.fold(newU1s)(newU1s +) -> false
        case ((u1s, _), entry) if u2 == entry.user => u1s -> {
          u1s(entry.ip.value) || entry.fp.??(u1s.contains)
        }
      }._2
    }

  private def recentUserIdsByField(field: String)(value: String): Fu[List[User.ID]] =
    coll.distinct[User.ID, List](
      "user",
      $doc(
        field -> value,
        "date" $gt DateTime.now.minusYears(1)
      ).some
    )
}

object SecurityApi {

  case class MustConfirmEmail(userId: User.ID) extends Exception
}
