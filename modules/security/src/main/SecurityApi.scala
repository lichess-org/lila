package lila.security

import com.softwaremill.tagging.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.{ Constraint, Invalid, Valid as FormValid, ValidationError }
import play.api.Mode
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*
import ornicar.scalalib.SecureRandom

import lila.common.{ ApiVersion, EmailAddress, HTTPRequest, IpAddress }
import lila.common.Form.into
import lila.db.dsl.{ *, given }
import lila.oauth.{ OAuthScope, OAuthServer, AccessToken }
import lila.user.User.{ ClearPassword, LoginCandidate }
import lila.user.{ User, UserRepo, Me }
import lila.socket.Socket.Sri
import lila.user.User.LoginCandidate.Result

final class SecurityApi(
    userRepo: UserRepo,
    store: Store,
    firewall: Firewall,
    cacheApi: lila.memo.CacheApi,
    geoIP: GeoIP,
    authenticator: lila.user.Authenticator,
    oAuthServer: OAuthServer,
    ip2proxy: Ip2Proxy,
    proxy2faSetting: lila.memo.SettingStore[lila.common.Strings] @@ Proxy2faSetting
)(using ec: Executor, mode: Mode):

  val AccessUri = "access_uri"

  private val usernameOrEmailMapping =
    lila.common.Form.cleanText(minLength = 2, maxLength = EmailAddress.maxLength).into[UserStrOrEmail]
  private val loginPasswordMapping = nonEmptyText.transform(ClearPassword(_), _.value)

  lazy val loginForm = Form:
    tuple(
      "username" -> usernameOrEmailMapping, // can also be an email
      "password" -> loginPasswordMapping
    )
  def loginFormFilled(login: UserStrOrEmail) = loginForm.fill(login -> ClearPassword(""))

  lazy val rememberForm = Form(single("remember" -> boolean))

  private def loadedLoginForm(candidate: Option[LoginCandidate]): Form[Result] =
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
          case Must2fa =>
            Invalid(Seq(ValidationError("2-Factor Authentication is required to log in from this network.")))
          case err => Invalid(Seq(ValidationError(err.toString)))
      })
    )

  private def must2fa(req: RequestHeader): Fu[Option[IsProxy]] =
    ip2proxy(HTTPRequest.ipAddress(req)).map: p =>
      p.name.exists(proxy2faSetting.get().value.has(_)) option p

  def loadLoginForm(str: UserStrOrEmail)(using req: RequestHeader): Fu[Form[LoginCandidate.Result]] =
    EmailAddress
      .from(str.value)
      .match
        case Some(email) => authenticator.loginCandidateByEmail(email.normalize)
        case None        => User.validateId(str into UserStr) so authenticator.loginCandidateById
      .map(_.filter(_.user isnt User.lichessId))
      .flatMap:
        _.so: candidate =>
          must2fa(req).map:
            _.fold(candidate.some): p =>
              lila.mon.security.login.proxy(p.value).increment()
              candidate.copy(must2fa = true).some
      .map(loadedLoginForm)

  private def authenticateCandidate(candidate: Option[LoginCandidate])(
      login: UserStrOrEmail,
      password: ClearPassword,
      token: Option[String]
  ): LoginCandidate.Result =
    import LoginCandidate.Result.*
    candidate.fold[LoginCandidate.Result](InvalidUsernameOrPassword): c =>
      val result = c(User.PasswordAndToken(password, token map User.TotpToken.apply))
      if result == BlankedPassword then
        lila.common.Bus.publish(c.user, "loginWithBlankedPassword")
        BlankedPassword
      else if mode == Mode.Prod && result.success && PasswordCheck.isWeak(password, login.value) then
        lila.common.Bus.publish(c.user, "loginWithWeakPassword")
        WeakPassword
      else result

  def saveAuthentication(userId: UserId, apiVersion: Option[ApiVersion])(using
      req: RequestHeader
  ): Fu[String] =
    userRepo mustConfirmEmail userId flatMap {
      if _ then fufail(SecurityApi MustConfirmEmail userId)
      else
        ip2proxy(HTTPRequest.ipAddress(req)).flatMap: proxy =>
          val sessionId = SecureRandom nextString 22
          proxy.name.foreach: p =>
            logger.info(s"Proxy login $p $userId")
          store.save(sessionId, userId, req, apiVersion, up = true, fp = none, proxy = proxy) inject sessionId
    }

  def saveSignup(userId: UserId, apiVersion: Option[ApiVersion], fp: Option[FingerPrint])(using
      req: RequestHeader
  ): Funit =
    val sessionId = SecureRandom nextString 22
    store.save(s"SIG-$sessionId", userId, req, apiVersion, up = false, fp = fp)

  private type AppealOrUser = Either[AppealUser, FingerPrintedUser]
  def restoreUser(req: RequestHeader): Fu[Option[AppealOrUser]] =
    firewall.accepts(req) so reqSessionId(req) so { sessionId =>
      appeal.authenticate(sessionId) match
        case Some(userId) => userRepo byId userId map2 { u => Left(AppealUser(Me(u))) }
        case None =>
          store.authInfo(sessionId) flatMapz { d =>
            userRepo me d.user dmap {
              _ map { me => Right(FingerPrintedUser(stripRolesOfCookieUser(me), d.hasFp)) }
            }
          }
      : Fu[Option[AppealOrUser]]
    }

  def oauthScoped(
      req: RequestHeader,
      required: lila.oauth.EndpointScopes
  ): Fu[OAuthServer.AuthResult] =
    oAuthServer
      .auth(req, required)
      .addEffect:
        case Right(access) => upsertOauth(access, req)
        case _             => ()
      .map(_.map(access => stripRolesOfOAuthUser(access.scoped)))

  private object upsertOauth:
    private val sometimes = lila.memo.OnceEvery.hashCode[AccessToken.Id](1.hour)
    def apply(access: OAuthScope.Access, req: RequestHeader): Unit =
      if access.scoped.scopes.intersects(OAuthScope.relevantToMods) && sometimes(access.tokenId) then
        val mobile = Mobile.LichessMobileUa.parse(req)
        store.upsertOAuth(access.user.id, access.tokenId, mobile, req)

  private lazy val nonModRoles: Set[String] = Permission.nonModPermissions.map(_.dbKey)

  private def stripRolesOfOAuthUser(scoped: OAuthScope.Scoped) =
    if scoped.scopes.has(_.Web.Mod) then scoped
    else scoped.copy(me = stripRolesOf(scoped.me))

  private def stripRolesOfCookieUser(me: Me) =
    if mode == Mode.Prod && me.totpSecret.isEmpty then stripRolesOf(me)
    else me

  private def stripRolesOf(me: Me) =
    if me.roles.nonEmpty
    then me.map(_.copy(roles = me.roles.filter(nonModRoles.contains)))
    else me

  def locatedOpenSessions(userId: UserId, nb: Int): Fu[List[LocatedSession]] =
    store.openSessions(userId, nb) map {
      _.map: session =>
        LocatedSession(session, geoIP(session.ip))
    }

  def dedup(userId: UserId, req: RequestHeader): Funit =
    reqSessionId(req).so(store.dedup(userId, _))

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req).soFu(store.setFingerPrint(_, fp))

  val sessionIdKey = "sessionId"

  def reqSessionId(req: RequestHeader): Option[String] =
    req.session.get(sessionIdKey) orElse req.headers.get(sessionIdKey)

  def recentUserIdsByFingerHash(fh: FingerHash) = recentUserIdsByField("fp")(fh.value)

  def recentUserIdsByIp(ip: IpAddress) = recentUserIdsByField("ip")(ip.value)

  export store.shareAnIpOrFp

  def ipUas(ip: IpAddress): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("ip" -> ip.value), _.sec)

  def printUas(fh: FingerHash): Fu[List[String]] =
    store.coll.distinctEasy[String, List]("ua", $doc("fp" -> fh.value), _.sec)

  private def recentUserIdsByField(field: String)(value: String): Fu[List[UserId]] =
    store.coll.distinctEasy[UserId, List](
      "user",
      $doc(
        field -> value,
        "date" $gt nowInstant.minusYears(1)
      ),
      _.sec
    )

  // special temporary auth for marked closed accounts so they can use appeal endpoints
  object appeal:

    private type SessionId = String

    private val prefix = "appeal:"

    private val store = cacheApi.notLoadingSync[SessionId, UserId](256, "security.session.appeal"):
      _.expireAfterAccess(2.days).build()

    def authenticate(sessionId: SessionId): Option[UserId] =
      sessionId.startsWith(prefix) so store.getIfPresent(sessionId)

    def saveAuthentication(userId: UserId): Fu[SessionId] =
      val sessionId = s"$prefix${SecureRandom nextString 22}"
      store.put(sessionId, userId)
      logger.info(s"Appeal login by $userId")
      fuccess(sessionId)

object SecurityApi:

  case class MustConfirmEmail(userId: UserId) extends Exception
