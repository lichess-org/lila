package lila.security

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.{ Constraint, Invalid, Valid as FormValid, ValidationError }
import play.api.Mode
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference
import ornicar.scalalib.SecureRandom

import lila.common.{ ApiVersion, EmailAddress, HTTPRequest, IpAddress }
import lila.common.Form.into
import lila.db.dsl.{ *, given }
import lila.oauth.{ OAuthScope, OAuthServer }
import lila.user.User.{ ClearPassword, LoginCandidate }
import lila.user.{ User, UserRepo }

final class SecurityApi(
    userRepo: UserRepo,
    store: Store,
    firewall: Firewall,
    cacheApi: lila.memo.CacheApi,
    geoIP: GeoIP,
    authenticator: lila.user.Authenticator,
    oAuthServer: lila.oauth.OAuthServer,
    tor: Tor
)(using ec: Executor, mode: Mode):

  val AccessUri = "access_uri"

  private val usernameOrEmailMapping =
    lila.common.Form.cleanText(minLength = 2, maxLength = EmailAddress.maxLength).into[UserStrOrEmail]
  private val loginPasswordMapping = nonEmptyText.transform(ClearPassword(_), _.value)

  lazy val loginForm = Form {
    tuple(
      "username" -> usernameOrEmailMapping, // can also be an email
      "password" -> loginPasswordMapping
    )
  }
  def loginFormFilled(login: UserStrOrEmail) = loginForm.fill(login -> ClearPassword(""))

  lazy val rememberForm = Form(single("remember" -> boolean))

  private def loadedLoginForm(candidate: Option[LoginCandidate]) =
    import LoginCandidate.Result.*
    Form(
      mapping(
        "username" -> usernameOrEmailMapping, // can also be an email
        "password" -> loginPasswordMapping,
        "token"    -> optional(nonEmptyText)
      )(authenticateCandidate(candidate)) {
        case Success(user) => (user.username into UserStrOrEmail, ClearPassword(""), none).some
        case _             => none
      }.verifying(Constraint { (t: LoginCandidate.Result) =>
        t match
          case Success(_) => FormValid
          case InvalidUsernameOrPassword =>
            Invalid(Seq(ValidationError("invalidUsernameOrPassword")))
          case BlankedPassword =>
            Invalid(Seq(ValidationError("blankedPassword")))
          case WeakPassword =>
            Invalid(
              Seq(ValidationError("This password is too easy to guess. Request a password reset email."))
            )
          case err => Invalid(Seq(ValidationError(err.toString)))
      })
    )

  def loadLoginForm(str: UserStrOrEmail): Fu[Form[LoginCandidate.Result]] = {
    EmailAddress.from(str.value) match
      case Some(email) => authenticator.loginCandidateByEmail(email.normalize)
      case None        => User.validateId(str into UserStr) ?? authenticator.loginCandidateById
  } map loadedLoginForm

  private def authenticateCandidate(candidate: Option[LoginCandidate])(
      login: UserStrOrEmail,
      password: ClearPassword,
      token: Option[String]
  ): LoginCandidate.Result =
    import LoginCandidate.Result.*
    candidate.fold[LoginCandidate.Result](InvalidUsernameOrPassword) { c =>
      val result = c(User.PasswordAndToken(password, token map User.TotpToken.apply))
      if result == BlankedPassword then
        lila.common.Bus.publish(c.user, "loginWithBlankedPassword")
        BlankedPassword
      else if mode == Mode.Prod && result.success && PasswordCheck.isWeak(password, login.value) then
        lila.common.Bus.publish(c.user, "loginWithWeakPassword")
        WeakPassword
      else result
    }

  def saveAuthentication(userId: UserId, apiVersion: Option[ApiVersion])(using
      req: RequestHeader
  ): Fu[String] =
    userRepo mustConfirmEmail userId flatMap {
      case true => fufail(SecurityApi MustConfirmEmail userId)
      case false =>
        val sessionId = SecureRandom nextString 22
        if (tor isExitNode HTTPRequest.ipAddress(req)) logger.info(s"Tor login $userId")
        store.save(sessionId, userId, req, apiVersion, up = true, fp = none) inject sessionId
    }

  def saveSignup(userId: UserId, apiVersion: Option[ApiVersion], fp: Option[FingerPrint])(using
      req: RequestHeader
  ): Funit =
    val sessionId = SecureRandom nextString 22
    store.save(s"SIG-$sessionId", userId, req, apiVersion, up = false, fp = fp)

  private type AppealOrUser = Either[AppealUser, FingerPrintedUser]
  def restoreUser(req: RequestHeader): Fu[Option[AppealOrUser]] =
    firewall.accepts(req) ?? reqSessionId(req) ?? { sessionId =>
      appeal.authenticate(sessionId) match {
        case Some(userId) => userRepo byId userId map2 { u => Left(AppealUser(u)) }
        case None =>
          store.authInfo(sessionId) flatMapz { d =>
            userRepo byId d.user dmap {
              _ map { u => Right(FingerPrintedUser(stripRolesOfCookieUser(u), d.hasFp)) }
            }
          }
      }: Fu[Option[AppealOrUser]]
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

  def locatedOpenSessions(userId: UserId, nb: Int): Fu[List[LocatedSession]] =
    store.openSessions(userId, nb) map {
      _.map { session =>
        LocatedSession(session, geoIP(session.ip))
      }
    }

  def dedup(userId: UserId, req: RequestHeader): Funit =
    reqSessionId(req) ?? { store.dedup(userId, _) }

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req) ?? { store.setFingerPrint(_, fp) map some }

  val sessionIdKey = "sessionId"

  def reqSessionId(req: RequestHeader): Option[String] =
    req.session.get(sessionIdKey) orElse req.headers.get(sessionIdKey)

  def recentUserIdsByFingerHash(fh: FingerHash) = recentUserIdsByField("fp")(fh.value)

  def recentUserIdsByIp(ip: IpAddress) = recentUserIdsByField("ip")(ip.value)

  def shareAnIpOrFp = store.shareAnIpOrFp

  def ipUas(ip: IpAddress): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("ip" -> ip.value), ReadPreference.secondaryPreferred)

  def printUas(fh: FingerHash): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("fp" -> fh.value), ReadPreference.secondaryPreferred)

  private def recentUserIdsByField(field: String)(value: String): Fu[List[UserId]] =
    store.coll.distinctEasy[UserId, List](
      "user",
      $doc(
        field -> value,
        "date" $gt nowInstant.minusYears(1)
      ),
      ReadPreference.secondaryPreferred
    )

  // special temporary auth for marked closed accounts so they can use appeal endpoints
  object appeal:

    private type SessionId = String

    private val prefix = "appeal:"

    private val store = cacheApi.notLoadingSync[SessionId, UserId](256, "security.session.appeal")(
      _.expireAfterAccess(2.days).build()
    )

    def authenticate(sessionId: SessionId): Option[UserId] =
      sessionId.startsWith(prefix) ?? store.getIfPresent(sessionId)

    def saveAuthentication(userId: UserId): Fu[SessionId] =
      val sessionId = s"$prefix${SecureRandom nextString 22}"
      store.put(sessionId, userId)
      logger.info(s"Appeal login by $userId")
      fuccess(sessionId)

object SecurityApi:

  case class MustConfirmEmail(userId: UserId) extends Exception
