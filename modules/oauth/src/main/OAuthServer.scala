package lila.oauth

import com.roundeights.hasher.Algo
import com.softwaremill.tagging.*
import play.api.mvc.{ RequestHeader, Result }

import lila.common.HTTPRequest
import lila.core.config.Secret
import lila.core.net.{ Bearer, UserAgent }
import lila.memo.SettingStore

final class OAuthServer(
    userApi: lila.core.user.UserApi,
    tokenApi: AccessTokenApi,
    originBlocklist: SettingStore[lila.core.data.Strings] @@ OriginBlocklist,
    mobileSecrets: List[Secret] @@ MobileSecrets
)(using mode: play.api.Mode)(using Executor):

  import OAuthServer.*

  def authReq(req: RequestHeader, accepted: EndpointScopes): AccessFu =
    val res = for
      bearer <- HTTPRequest.bearer(req).raiseIfNone(MissingAuthorizationHeader)
      res <- auth(bearer, accepted, req.some)
      _ <- checkOauthUaUser(res, HTTPRequest.userAgent(req)).raiseIfSome(funit)
    yield res
    res.onComplete(x => monitorAuth(x.isSuccess))
    res

  def auth(tokenId: Bearer, accepted: EndpointScopes, andLogReq: Option[RequestHeader]): AccessFu = for
    at <- getTokenFromSignedBearer(tokenId)
    at <- at.raiseIfNone(NoSuchToken)
    _ <- raiseIf(!accepted.isEmpty && !accepted.compatible(at.scopes)):
      MissingScope(accepted, at.scopes)
    u <- userApi.me(at.userId)
    u <- u.raiseIfNone(NoSuchUser)
    blocked = at.clientOrigin.exists(origin => originBlocklist.get().value.exists(origin.contains))
    _ = andLogReq
      .filter: req =>
        blocked || (u.isnt(UserId.explorer) && !HTTPRequest.looksLikeLichessBot(req))
      .foreach: req =>
        def logMsg =
          s"${if blocked then "block" else "auth"} ${at.clientOrigin | "-"} as ${u.username} ${HTTPRequest.print(req).take(200)}"
        if blocked then logger.info(logMsg) else logger.debug(logMsg)
    _ <- raiseIf(blocked)(OriginBlocked)
  yield OAuthScope.Access(OAuthScope.Scoped(u, at.scopes), at.tokenId)

  def authBoth(scopes: EndpointScopes, req: RequestHeader)(
      token1: Bearer,
      token2: Bearer
  ): FuRaise[AuthError, (User, User)] = for
    auth1 <- auth(token1, scopes, req.some)
    auth2 <- auth(token2, scopes, req.some)
    _ <- raiseIf(auth1.user.is(auth2.user))(OneUserWithTwoTokens)
  yield auth1.user -> auth2.user

  val UaUserRegex = """(?:user|as):\s?([\w\-]{3,31})""".r
  private def checkOauthUaUser(access: OAuthScope.Access, ua: UserAgent): Option[AuthError] =
    ua.value match
      case UaUserRegex(u) if access.me.isnt(UserStr(u)) => UserAgentMismatch.some
      case _ => none

  private val bearerSigners = mobileSecrets.map(s => Algo.hmac(s.value))

  private def checkSignedBearer(bearer: String, signed: String): Boolean =
    bearerSigners.exists: signer =>
      signer.sha1(bearer).hash_=(signed)

  private def getTokenFromSignedBearer(full: Bearer): Fu[Option[AccessToken.ForAuth]] =
    val (bearer, signed) = full.value.split(':') match
      case Array(bearer, signed) if checkSignedBearer(bearer, signed) => (Bearer(bearer), true)
      case _ => (full, false)
    tokenApi
      .get(bearer)
      .mapz: token =>
        if token.scopes.has(_.Web.Mobile) && !signed then
          logger.warn(s"Web:Mobile token requested but not signed: $token")
          mode.isDev.option(token)
        else if token.scopes.has(_.Web.Mobile) && !token.clientOrigin.has("org.lichess.mobile://") then
          logger.warn(s"Web:Mobile token requested but invalid origin: $token")
          mode.isDev.option(token)
        else token.some

  private def monitorAuth(success: Boolean) =
    lila.mon.user.oauth.request(success).increment()

object OAuthServer:

  type AccessFu = FuRaise[AuthError, OAuthScope.Access]
  type AuthFu = FuRaise[AuthError, OAuthScope.Scoped]

  class AuthError(val message: String)
  case object MissingAuthorizationHeader extends AuthError("Missing authorization header")
  case object NoSuchToken extends AuthError("No such token")
  case class MissingScope(accepted: EndpointScopes, available: TokenScopes)
      extends AuthError(s"Missing scope: ${accepted.show}")
  case object NoSuchUser extends AuthError("No such user")
  case object OneUserWithTwoTokens extends AuthError("Both tokens belong to the same user")
  case object OriginBlocked extends AuthError("Origin blocked")
  case object UserAgentMismatch extends AuthError("The user in the user-agent doesn't match the token bearer")

  def responseHeaders(accepted: EndpointScopes, tokenScopes: TokenScopes)(res: Result): Result =
    res.withHeaders(
      "X-OAuth-Scopes" -> tokenScopes.into(OAuthScopes).keyList,
      "X-Accepted-OAuth-Scopes" -> accepted.into(OAuthScopes).keyList
    )
