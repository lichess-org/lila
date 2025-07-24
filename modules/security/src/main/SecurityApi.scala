package lila.security

import com.softwaremill.tagging.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.{ Constraint, Invalid, Valid as FormValid, ValidationError }
import play.api.mvc.{ RequestHeader, Session }
import reactivemongo.api.bson.*
import scalalib.SecureRandom

import lila.common.Form.into
import lila.common.HTTPRequest
import lila.core.id.SessionId
import lila.core.mod.{ LoginWithBlankedPassword, LoginWithWeakPassword }
import lila.core.email.UserStrOrEmail
import lila.core.net.{ ApiVersion, IpAddress }
import lila.core.misc.oauth.AccessTokenId
import lila.core.security.{ ClearPassword, FingerHash, Ip2ProxyApi, IsProxy }
import lila.db.dsl.{ *, given }
import lila.oauth.{ AccessToken, OAuthScope, OAuthServer }
import lila.security.LoginCandidate.Result
import lila.core.user.RoleDbKey

final class SecurityApi(
    userRepo: lila.user.UserRepo,
    store: SessionStore,
    firewall: Firewall,
    cacheApi: lila.memo.CacheApi,
    geoIP: GeoIP,
    authenticator: Authenticator,
    oAuthServer: OAuthServer,
    ip2proxy: Ip2ProxyApi,
    proxy2faSetting: lila.memo.SettingStore[lila.core.data.Strings] @@ Proxy2faSetting
)(using ec: Executor, mode: play.api.Mode):

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
        case Success(user) => (user.username.into(UserStrOrEmail), ClearPassword(""), none).some
        case _             => none
      }.verifying(Constraint { (t: LoginCandidate.Result) =>
        t match
          case Success(_)                => FormValid
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
    ip2proxy
      .ofReq(req)
      .map: p =>
        p.name.exists(proxy2faSetting.get().value.has(_)).option(p)

  def loadLoginForm(str: UserStrOrEmail)(using req: RequestHeader): Fu[Form[LoginCandidate.Result]] =
    EmailAddress
      .from(str.value)
      .match
        case Some(email) => authenticator.loginCandidateByEmail(email.normalize)
        case None        => str.into(UserStr).validateId.so(authenticator.loginCandidateById)
      .map(_.filter(_.user.isnt(UserId.lichess)))
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
      val result = c(PasswordAndToken(password, token.map(lila.user.TotpToken.apply)))
      if result == BlankedPassword then
        lila.common.Bus.pub(LoginWithBlankedPassword(c.user.id))
        BlankedPassword
      else if mode.isProd && result.success && PasswordCheck.isWeak(password, login.value) then
        lila.common.Bus.pub(LoginWithWeakPassword(c.user.id))
        WeakPassword
      else result

  def saveAuthentication(userId: UserId, apiVersion: Option[ApiVersion])(using
      req: RequestHeader
  ): Fu[SessionId] =
    userRepo
      .mustConfirmEmail(userId)
      .flatMap:
        if _ then fufail(SecurityApi.MustConfirmEmail(userId))
        else
          for
            proxy <- ip2proxy.ofReq(req)
            _ = proxy.name.foreach(p => logger.info(s"Proxy login $p $userId ${HTTPRequest.print(req)}"))
            sessionId = SessionId(SecureRandom.nextString(22))
            _ <- store.save(sessionId, userId, req, apiVersion, up = true, fp = none, proxy = proxy)
          yield sessionId

  def saveSignup(userId: UserId, apiVersion: Option[ApiVersion], fp: Option[FingerPrint])(using
      req: RequestHeader
  ): Funit =
    for
      proxy <- ip2proxy.ofReq(req)
      sessionId = SessionId(s"SIG-${SecureRandom.nextString(22)}")
      _ <- store.save(sessionId, userId, req, apiVersion, up = false, fp = fp, proxy = proxy)
    yield ()

  private type AppealOrUser = Either[AppealUser, FingerPrintedUser]
  def restoreUser(req: RequestHeader): Fu[Option[AppealOrUser]] =
    if HTTPRequest.isXhrFromEmbed(req) then fuccess(none)
    else
      firewall.accepts(req).so(reqSessionId(req)).so { sessionId =>
        appeal.authenticate(sessionId) match
          case Some(userId) => userRepo.byId(userId).map2 { u => Left(AppealUser(Me(u))) }
          case None         =>
            store.authInfo(sessionId).flatMapz { d =>
              userRepo
                .me(d.user)
                .dmap:
                  _.map { me => Right(FingerPrintedUser(stripRolesOfCookieUser(me), d.hasFp)) }
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
    private val sometimes = scalalib.cache.OnceEvery.hashCode[AccessTokenId](1.hour)
    def apply(access: OAuthScope.Access, req: RequestHeader): Unit =
      if access.scoped.scopes.intersects(OAuthScope.relevantToMods) && sometimes(access.tokenId) then
        val mobile = Mobile.LichessMobileUa.parse(req)
        store.upsertOAuth(access.me.userId, access.tokenId, mobile, req)

  private lazy val nonModRoles: Set[RoleDbKey] = lila.core.perm.Permission.nonModPermissions.map(_.dbKey)

  private def stripRolesOfOAuthUser(scoped: OAuthScope.Scoped) =
    if scoped.scopes.has(_.Web.Mod) then scoped
    else scoped.copy(me = stripRolesOf(scoped.me))

  private def stripRolesOfCookieUser(me: Me) =
    if mode.isProd && me.totpSecret.isEmpty then stripRolesOf(me)
    else me

  private def stripRolesOf(me: Me) =
    if me.roles.nonEmpty
    then me.map(_.copy(roles = me.roles.filter(nonModRoles.contains)))
    else me

  def locatedOpenSessions(userId: UserId, nb: Int): Fu[List[LocatedSession]] =
    store
      .openSessions(userId, nb)
      .map:
        _.map: session =>
          LocatedSession(session, geoIP(session.ip))

  def dedup(userId: UserId, req: RequestHeader): Funit =
    reqSessionId(req).so(store.dedup(userId, _))

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req).soFu(store.setFingerPrint(_, fp))

  val sessionIdKey = "sessionId"

  def reqSessionId(req: RequestHeader): Option[SessionId] =
    import play.api.mvc.request.{ Cell, RequestAttrKey }
    req.attrs.get[Cell[Session]](RequestAttrKey.Session) match
      case Some(session) =>
        session.value.get(sessionIdKey).orElse(req.headers.get(sessionIdKey)).map(SessionId.apply)
      case None =>
        logger.warn(s"No session in request attrs: ${HTTPRequest.print(req)}")
        none

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
        "date".$gt(nowInstant.minusYears(1))
      ),
      _.sec
    )

  // special temporary auth for marked closed accounts so they can use appeal endpoints
  object appeal:

    private val prefix = "appeal:"

    private val store = cacheApi.notLoadingSync[SessionId, UserId](256, "security.session.appeal"):
      _.expireAfterAccess(2.days).build()

    def authenticate(sessionId: SessionId): Option[UserId] =
      sessionId.value.startsWith(prefix).so(store.getIfPresent(sessionId))

    def saveAuthentication(userId: UserId): Fu[SessionId] =
      val sessionId = SessionId(s"$prefix${SecureRandom.nextString(22)}")
      store.put(sessionId, userId)
      logger.info(s"Appeal login by $userId")
      fuccess(sessionId)

object SecurityApi:

  case class MustConfirmEmail(userId: UserId) extends Exception
