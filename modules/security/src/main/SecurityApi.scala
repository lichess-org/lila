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
import lila.core.email.UserStrOrEmail
import lila.core.net.{ ApiVersion, IpAddress }
import lila.core.security.{ ClearPassword, FingerHash, Ip2ProxyApi, IsProxy }
import lila.db.dsl.{ *, given }
import lila.oauth.{ AccessToken, OAuthScope, OAuthServer }
import lila.core.user.RoleDbKey

final class SecurityApi(
    userRepo: lila.user.UserRepo,
    store: Store,
    firewall: Firewall,
    cacheApi: lila.memo.CacheApi,
    geoIP: GeoIP,
    oAuthServer: OAuthServer,
    ip2proxy: Ip2ProxyApi
)(using ec: Executor):

  val AccessUri = "access_uri"

  def saveAuthentication(
      userId: UserId,
      apiVersion: Option[ApiVersion],
      checkEmailConfirmation: Boolean = true,
      oidcSession: Option[lila.oidc.OidcSession] = None
  )(using req: RequestHeader): Fu[String] =
    (if checkEmailConfirmation then userRepo.mustConfirmEmail(userId) else fuccess(false)).flatMap {
      if _ then fufail(SecurityApi.MustConfirmEmail(userId))
      else
        for
          proxy <- ip2proxy(HTTPRequest.ipAddress(req))
          _ = proxy.name.foreach(p => logger.info(s"Proxy login $p $userId ${HTTPRequest.print(req)}"))
          sessionId = SecureRandom.nextString(22)
          _ <- store.save(
            sessionId,
            userId,
            req,
            apiVersion,
            up = true,
            fp = none,
            proxy = proxy,
            oidcSession = oidcSession
          )
        yield sessionId
    }

  private type AppealOrUser = Either[AppealUser, FingerPrintedUser]
  def restoreUser(req: RequestHeader): Fu[Option[AppealOrUser]] =
    if HTTPRequest.isXhrFromEmbed(req) then fuccess(none)
    else
      firewall.accepts(req).so(reqSessionId(req)).so { sessionId =>
        appeal.authenticate(sessionId) match
          case Some(userId) => userRepo.byId(userId).map2 { u => Left(AppealUser(Me(u))) }
          case None =>
            store.authInfo(sessionId).flatMapz { d =>
              userRepo.me(d.user).dmap {
                _.map { me => Right(FingerPrintedUser(stripRolesOfCookieUser(me), d.hasFp)) }
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
    private val sometimes = scalalib.cache.OnceEvery.hashCode[AccessToken.Id](1.hour)
    def apply(access: OAuthScope.Access, req: RequestHeader): Unit =
      if access.scoped.scopes.intersects(OAuthScope.relevantToMods) && sometimes(access.tokenId) then
        val mobile = Mobile.LichessMobileUa.parse(req)
        store.upsertOAuth(access.me.userId, access.tokenId, mobile, req)

  private lazy val nonModRoles: Set[RoleDbKey] = lila.core.perm.Permission.nonModPermissions.map(_.dbKey)

  private def stripRolesOfOAuthUser(scoped: OAuthScope.Scoped) =
    if scoped.scopes.has(_.Web.Mod) then scoped
    else scoped.copy(me = stripRolesOf(scoped.me))

  private def stripRolesOfCookieUser(me: Me) =
    // if mode.isProd && me.totpSecret.isEmpty then stripRolesOf(me)
    // else me
    me

  private def stripRolesOf(me: Me) =
    if me.roles.nonEmpty
    then me.map(_.copy(roles = me.roles.filter(nonModRoles.contains)))
    else me

  def locatedOpenSessions(userId: UserId, nb: Int): Fu[List[LocatedSession]] =
    store.openSessions(userId, nb).map {
      _.map: session =>
        LocatedSession(session, geoIP(session.ip))
    }

  def dedup(userId: UserId, req: RequestHeader): Funit =
    reqSessionId(req).so(store.dedup(userId, _))

  def setFingerPrint(req: RequestHeader, fp: FingerPrint): Fu[Option[FingerHash]] =
    reqSessionId(req).soFu(store.setFingerPrint(_, fp))

  val sessionIdKey = "sessionId"

  def reqSessionId(req: RequestHeader): Option[String] =
    import play.api.mvc.request.{ Cell, RequestAttrKey }
    req.attrs.get[Cell[Session]](RequestAttrKey.Session) match
      case Some(session) => session.value.get(sessionIdKey).orElse(req.headers.get(sessionIdKey))
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

    private type SessionId = String

    private val prefix = "appeal:"

    private val store = cacheApi.notLoadingSync[SessionId, UserId](256, "security.session.appeal"):
      _.expireAfterAccess(2.days).build()

    def authenticate(sessionId: SessionId): Option[UserId] =
      sessionId.startsWith(prefix).so(store.getIfPresent(sessionId))

    def saveAuthentication(userId: UserId): Fu[SessionId] =
      val sessionId = s"$prefix${SecureRandom.nextString(22)}"
      store.put(sessionId, userId)
      logger.info(s"Appeal login by $userId")
      fuccess(sessionId)

    def delete(sessionId: SessionId): Funit =
      store.invalidate(sessionId)
      funit

    def closeAllOf(userId: UserId): Funit =
      store
        .asMap()
        .foreach:
          case (sessionId, storedUserId) if storedUserId == userId => store.invalidate(sessionId)
          case _                                                   => ()
      funit

object SecurityApi:

  case class MustConfirmEmail(userId: UserId) extends Exception
