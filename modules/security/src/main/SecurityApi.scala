package lila.security

import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{ Constraint, Valid => FormValid, Invalid, ValidationError }
import play.api.mvc.RequestHeader
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.common.{ ApiVersion, IpAddress, EmailAddress, HTTPRequest }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.oauth.OAuthServer
import lila.user.{ User, UserRepo }
import User.LoginCandidate

final class SecurityApi(
    coll: Coll,
    firewall: Firewall,
    geoIP: GeoIP,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator,
    tryOauthServer: lila.oauth.OAuthServer.Try
)(implicit system: akka.actor.ActorSystem) {

  val AccessUri = "access_uri"

  lazy val usernameOrEmailForm = Form(single(
    "username" -> nonEmptyText
  ))

  lazy val loginForm = Form(tuple(
    "username" -> nonEmptyText, // can also be an email
    "password" -> nonEmptyText
  ))

  private def loadedLoginForm(candidate: Option[LoginCandidate]) = Form(mapping(
    "username" -> nonEmptyText, // can also be an email
    "password" -> nonEmptyText,
    "token" -> optional(nonEmptyText)
  )(authenticateCandidate(candidate)) {
      case LoginCandidate.Success(user) => (user.username, "", none).some
      case _ => none
    }.verifying(Constraint { (t: LoginCandidate.Result) =>
      t match {
        case LoginCandidate.Success(_) => FormValid
        case LoginCandidate.InvalidUsernameOrPassword => Invalid(Seq(ValidationError("invalidUsernameOrPassword")))
        case err => Invalid(Seq(ValidationError(err.toString)))
      }
    }))

  def loadLoginForm(str: String): Fu[Form[LoginCandidate.Result]] = {
    emailValidator.validate(EmailAddress(str)) match {
      case Some(EmailAddressValidator.Acceptable(email)) => authenticator.loginCandidateByEmail(email.normalize)
      case None if User.couldBeUsername(str) => authenticator.loginCandidateById(User normalize str)
      case _ => fuccess(none)
    }
  } map loadedLoginForm _

  private def authenticateCandidate(candidate: Option[LoginCandidate])(
    username: String,
    password: String,
    token: Option[String]
  ): LoginCandidate.Result = candidate.fold[LoginCandidate.Result](LoginCandidate.InvalidUsernameOrPassword) {
    _(User.PasswordAndToken(User.ClearPassword(password), token map User.TotpToken.apply))
  }

  def saveAuthentication(userId: User.ID, apiVersion: Option[ApiVersion])(implicit req: RequestHeader): Fu[String] =
    UserRepo mustConfirmEmail userId flatMap {
      case true => fufail(SecurityApi MustConfirmEmail userId)
      case false =>
        val sessionId = Random secureString 22
        Store.save(sessionId, userId, req, apiVersion, up = true, fp = none) inject sessionId
    }

  def saveSignup(userId: User.ID, apiVersion: Option[ApiVersion], fp: Option[FingerPrint])(implicit req: RequestHeader): Funit = {
    val sessionId = Random secureString 22
    Store.save(s"SIG-$sessionId", userId, req, apiVersion, up = false, fp = fp)
  }

  def restoreUser(req: RequestHeader): Fu[Option[FingerprintedUser]] =
    firewall.accepts(req) ?? {
      reqSessionId(req).?? { sessionId =>
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

  def oauthScoped(req: RequestHeader, scopes: List[lila.oauth.OAuthScope], retries: Int = 2): Fu[lila.oauth.OAuthServer.AuthResult] =
    tryOauthServer().flatMap {
      case None if retries > 0 =>
        lila.common.Future.delay(2 seconds) {
          oauthScoped(req, scopes, retries - 1)
        }
      case None => fuccess(Left(OAuthServer.ServerOffline))
      case Some(server) => server.auth(req, scopes)
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

  val sessionIdKey = "sessionId"

  private def isMobileAppWS(req: RequestHeader) =
    HTTPRequest.isSocket(req) && HTTPRequest.origin(req).fold(true)("file://" ==)

  def reqSessionId(req: RequestHeader): Option[String] =
    req.session.get(sessionIdKey) orElse
      req.headers.get(sessionIdKey) orElse {
        isMobileAppWS(req) ?? req.queryString.get(sessionIdKey).flatMap(_.headOption)
      }

  def userIdsSharingIp = userIdsSharingField("ip") _

  def recentByIpExists(ip: IpAddress): Fu[Boolean] = Store recentByIpExists ip

  def recentByPrintExists(fp: FingerPrint): Fu[Boolean] = Store recentByPrintExists fp

  private def userIdsSharingField(field: String)(userId: User.ID): Fu[List[User.ID]] =
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
    Store.ipsAndFps(List(u1, u2), max = 100) map { ipsAndFps =>
      val u1s: Set[String] = ipsAndFps.filter(_.user == u1).flatMap { x =>
        List(x.ip.value, ~x.fp)
      }.toSet
      ipsAndFps.exists { x =>
        x.user == u2 && {
          u1s(x.ip.value) || x.fp.??(u1s.contains)
        }
      }
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
