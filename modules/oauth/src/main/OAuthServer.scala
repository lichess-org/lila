package lila.oauth

import com.roundeights.hasher.Algo
import com.softwaremill.tagging.*
import play.api.mvc.{ RequestHeader, Result }

import lila.common.HTTPRequest
import lila.core.config.Secret
import lila.core.net.Bearer
import lila.memo.SettingStore

final class OAuthServer(
    userApi: lila.core.user.UserApi,
    tokenApi: AccessTokenApi,
    originBlocklist: SettingStore[lila.core.data.Strings] @@ OriginBlocklist,
    mobileSecrets: List[Secret] @@ MobileSecrets
)(using Executor):

  import OAuthServer.*

  def auth(req: RequestHeader, accepted: EndpointScopes): Fu[AccessResult] =
    HTTPRequest
      .bearer(req)
      .fold(fufail(MissingAuthorizationHeader)):
        auth(_, accepted, req.some)
      .map(checkOauthUaUser(req))
      .recover:
        case e: AuthError => Left(e)
      .addEffect: res =>
        monitorAuth(res.isRight)

  def auth(
      tokenId: Bearer,
      accepted: EndpointScopes,
      andLogReq: Option[RequestHeader]
  ): Fu[AccessResult] =
    getTokenFromSignedBearer(tokenId)
      .orFailWith(NoSuchToken)
      .flatMap:
        case at if !accepted.isEmpty && !accepted.compatible(at.scopes) =>
          fufail(MissingScope(accepted, at.scopes))
        case at =>
          userApi
            .me(at.userId)
            .flatMap:
              case None => fufail(NoSuchUser)
              case Some(u) =>
                val blocked =
                  at.clientOrigin.exists(origin => originBlocklist.get().value.exists(origin.contains))
                andLogReq
                  .filter: req =>
                    blocked || {
                      u.isnt(UserId.explorer) && !HTTPRequest.looksLikeLichessBot(req)
                    }
                  .foreach: req =>
                    def logMsg =
                      s"${if blocked then "block" else "auth"} ${at.clientOrigin | "-"} as ${u.username} ${HTTPRequest.print(req).take(200)}"
                    if blocked
                    then logger.info(logMsg)
                    else logger.debug(logMsg)
                if blocked then fufail(OriginBlocked)
                else fuccess(OAuthScope.Access(OAuthScope.Scoped(u, at.scopes), at.tokenId))
      .dmap(Right(_))
      .recover:
        case e: AuthError => Left(e)

  def authBoth(scopes: EndpointScopes, req: RequestHeader)(
      token1: Bearer,
      token2: Bearer
  ): Fu[Either[AuthError, (User, User)]] = for
    auth1 <- auth(token1, scopes, req.some)
    auth2 <- auth(token2, scopes, req.some)
  yield for
    user1 <- auth1
    user2 <- auth2
    result <-
      if user1.user.is(user2.user)
      then Left(OneUserWithTwoTokens)
      else Right(user1.user -> user2.user)
  yield result

  val UaUserRegex = """(?:user|as):\s?([\w\-]{3,31})""".r
  private def checkOauthUaUser(req: RequestHeader)(access: AccessResult): AccessResult =
    access.flatMap: a =>
      HTTPRequest.userAgent(req).map(_.value) match
        case Some(UaUserRegex(u)) if a.me.isnt(UserStr(u)) => Left(UserAgentMismatch)
        case _ => Right(a)

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
          none
        else token.some

  private def monitorAuth(success: Boolean) =
    lila.mon.user.oauth.request(success).increment()

object OAuthServer:

  type AccessResult = Either[AuthError, OAuthScope.Access]
  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.core.lilaism.LilaException
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
