package lila.security

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{ Constraint, Valid => FormValid, Invalid, ValidationError }
import play.api.Mode
import play.api.mvc.RequestHeader
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.annotation.nowarn
import scala.concurrent.duration._

import lila.common.{ ApiVersion, Bearer, EmailAddress, HTTPRequest, IpAddress, SecureRandom }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.oauth.{ AccessToken, OAuthScope, OAuthServer }
import lila.user.User.LoginCandidate
import lila.user.{ User, UserRepo }

final class SecurityApi(
    userRepo: UserRepo,
    store: Store,
    firewall: Firewall,
    cacheApi: lila.memo.CacheApi,
    geoIP: GeoIP,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator,
    oAuthServer: lila.oauth.OAuthServer,
    tor: Tor
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: Mode
) {

  val AccessUri = "access_uri"

  lazy val usernameOrEmailForm = Form(
    single(
      "username" -> nonEmptyText
    )
  )

  lazy val loginForm = Form(
    tuple(
      "username" -> nonEmptyText, // can also be an email
      "password" -> nonEmptyText
    )
  )

  private def loadedLoginForm(candidate: Option[LoginCandidate]) =
    Form(
      mapping(
        "username" -> nonEmptyText, // can also be an email
        "password" -> nonEmptyText,
        "token"    -> optional(nonEmptyText)
      )(authenticateCandidate(candidate)) {
        case LoginCandidate.Success(user) => (user.username, "", none).some
        case _                            => none
      }.verifying(Constraint { (t: LoginCandidate.Result) =>
        t match {
          case LoginCandidate.Success(_) => FormValid
          case LoginCandidate.InvalidUsernameOrPassword =>
            Invalid(Seq(ValidationError("invalidUsernameOrPassword")))
          case err => Invalid(Seq(ValidationError(err.toString)))
        }
      })
    )

  def loadLoginForm(str: String): Fu[Form[LoginCandidate.Result]] = {
    emailValidator.validate(EmailAddress(str)) match {
      case Some(EmailAddressValidator.Acceptable(email)) =>
        authenticator.loginCandidateByEmail(email.normalize)
      case None if User.couldBeUsername(str) => authenticator.loginCandidateById(User normalize str)
      case _                                 => fuccess(none)
    }
  } map loadedLoginForm

  @nowarn("cat=unused")
  private def authenticateCandidate(candidate: Option[LoginCandidate])(
      _username: String,
      password: String,
      token: Option[String]
  ): LoginCandidate.Result =
    candidate.fold[LoginCandidate.Result](LoginCandidate.InvalidUsernameOrPassword) {
      _(User.PasswordAndToken(User.ClearPassword(password), token map User.TotpToken.apply))
    }

  def saveAuthentication(userId: User.ID, apiVersion: Option[ApiVersion])(implicit
      req: RequestHeader
  ): Fu[String] =
    userRepo mustConfirmEmail userId flatMap {
      case true => fufail(SecurityApi MustConfirmEmail userId)
      case false =>
        val sessionId = SecureRandom nextString 22
        if (tor isExitNode HTTPRequest.ipAddress(req)) logger.info(s"Tor login $userId")
        store.save(sessionId, userId, req, apiVersion, up = true, fp = none) inject sessionId
    }

  def saveSignup(userId: User.ID, apiVersion: Option[ApiVersion], fp: Option[FingerPrint])(implicit
      req: RequestHeader
  ): Funit = {
    val sessionId = SecureRandom nextString 22
    store.save(s"SIG-$sessionId", userId, req, apiVersion, up = false, fp = fp)
  }

  def restoreUser(req: RequestHeader): Fu[Option[Either[AppealUser, FingerPrintedUser]]] =
    firewall.accepts(req) ?? reqSessionId(req) ?? { sessionId =>
      appeal.authenticate(sessionId) match {
        case Some(userId) => userRepo byId userId map2 { u => Left(AppealUser(u)) }
        case None =>
          store.authInfo(sessionId) flatMap {
            _ ?? { d =>
              userRepo byId d.user dmap {
                _ map { u => Right(FingerPrintedUser(stripRolesOfCookieUser(u), d.hasFp)) }
              }
            }
          }
      }
    }

  def oauthScoped(
      req: RequestHeader,
      scopes: List[lila.oauth.OAuthScope]
  ): Fu[lila.oauth.OAuthServer.AuthResult] =
    oAuthServer.auth(req, scopes) map { _ map stripRolesOfOAuthUser }

  private lazy val nonModRoles: Set[String] = Permission.nonModPermissions.map(_.dbKey)

  private def stripRolesOfOAuthUser(scoped: OAuthScope.Scoped) =
    if (scoped.scopes has OAuthScope.Web.Mod) scoped
    else scoped.copy(user = stripRolesOfUser(scoped.user))

  private def stripRolesOfCookieUser(user: User) =
    if (mode == Mode.Prod && user.totpSecret.isEmpty) stripRolesOfUser(user)
    else user

  private def stripRolesOfUser(user: User) = user.copy(roles = user.roles.filter(nonModRoles.contains))

  def locatedOpenSessions(userId: User.ID, nb: Int): Fu[List[LocatedSession]] =
    store.openSessions(userId, nb) map {
      _.map { session =>
        LocatedSession(session, geoIP(session.ip))
      }
    }

  def dedup(userId: User.ID, req: RequestHeader): Funit =
    reqSessionId(req) ?? { store.dedup(userId, _) }

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req) ?? { store.setFingerPrint(_, fp) map some }

  val sessionIdKey = "sessionId"

  def reqSessionId(req: RequestHeader): Option[String] =
    req.session.get(sessionIdKey) orElse req.headers.get(sessionIdKey)

  def recentUserIdsByFingerHash(fh: FingerHash) = recentUserIdsByField("fp")(fh.value)

  def recentUserIdsByIp(ip: IpAddress) = recentUserIdsByField("ip")(ip.value)

  def shareAnIpOrFp = store.shareAnIpOrFp _

  def ipUas(ip: IpAddress): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("ip" -> ip.value), ReadPreference.secondaryPreferred)

  def printUas(fh: FingerHash): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("fp" -> fh.value), ReadPreference.secondaryPreferred)

  private def recentUserIdsByField(field: String)(value: String): Fu[List[User.ID]] =
    store.coll.distinctEasy[User.ID, List](
      "user",
      $doc(
        field -> value,
        "date" $gt DateTime.now.minusYears(1)
      ),
      ReadPreference.secondaryPreferred
    )

  // special temporary auth for marked closed accounts so they can use appeal endpoints
  object appeal {

    private type SessionId = String

    private val prefix = "appeal:"

    private val store = cacheApi.notLoadingSync[SessionId, User.ID](256, "security.session.appeal")(
      _.expireAfterAccess(2.days).build()
    )

    def authenticate(sessionId: SessionId): Option[User.ID] =
      sessionId.startsWith(prefix) ?? store.getIfPresent(sessionId)

    def saveAuthentication(userId: User.ID)(implicit req: RequestHeader): Fu[SessionId] = {
      val sessionId = s"$prefix${SecureRandom nextString 22}"
      store.put(sessionId, userId)
      logger.info(s"Appeal login by $userId")
      fuccess(sessionId)
    }
  }
}

object SecurityApi {

  case class MustConfirmEmail(userId: User.ID) extends Exception
}
