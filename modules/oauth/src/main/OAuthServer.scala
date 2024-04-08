package lila.oauth

import com.softwaremill.tagging.*
import play.api.mvc.{ RequestHeader, Result }
import com.roundeights.hasher.Algo

import lila.common.HTTPRequest
import lila.core.{ Bearer, Strings }
import lila.memo.SettingStore
import lila.core.user.User
import lila.core.config.Secret

final class OAuthServer(
    tokenApi: AccessTokenApi,
    originBlocklist: SettingStore[Strings] @@ OriginBlocklist,
    mobileSecret: Secret @@ MobileSecret
)(using Executor):

  import OAuthServer.*

  def auth[U: UserIdOf](req: RequestHeader, accepted: EndpointScopes)(using
      FetchUser[U]
  ): Fu[AccessResult[U]] =
    HTTPRequest
      .bearer(req)
      .fold(fufail(MissingAuthorizationHeader)):
        auth(_, accepted, req.some)
      .map(checkOauthUaUser(req))
      .recover:
        case e: AuthError => Left(e)
      .addEffect: res =>
        monitorAuth(res.isRight)

  def auth[U: UserIdOf](
      tokenId: Bearer,
      accepted: EndpointScopes,
      andLogReq: Option[RequestHeader]
  )(using fetchUser: FetchUser[U]): Fu[AccessResult[U]] =
    getTokenFromSignedBearer(tokenId)
      .orFailWith(NoSuchToken)
      .flatMap {
        case at if !accepted.isEmpty && !accepted.compatible(at.scopes) =>
          fufail(MissingScope(at.scopes))
        case at =>
          fetchUser(at.userId).flatMap {
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
                  logger.debug:
                    s"${if blocked then "block" else "auth"} ${at.clientOrigin | "-"} as ${u.id} ${HTTPRequest.print(req).take(200)}"
              if blocked then fufail(OriginBlocked)
              else fuccess(OAuthScope.Access(OAuthScope.Scoped(u, at.scopes), at.tokenId))
          }
      }
      .dmap(Right.apply)
      .recover { case e: AuthError =>
        Left(e)
      }

  def authBoth[U: UserIdOf](scopes: EndpointScopes, req: RequestHeader)(
      token1: Bearer,
      token2: Bearer
  )(using FetchUser[U]): Fu[Either[AuthError, (U, U)]] = for
    auth1 <- auth(token1, scopes, req.some)
    auth2 <- auth(token2, scopes, req.some)
  yield for
    user1 <- auth1
    user2 <- auth2
    result <-
      if user1.me.is(user2.me)
      then Left(OneUserWithTwoTokens)
      else Right(user1.me -> user2.me)
  yield result

  val UaUserRegex = """(?:user|as):\s?([\w\-]{3,31})""".r
  private def checkOauthUaUser[U: UserIdOf](req: RequestHeader)(access: AccessResult[U]): AccessResult[U] =
    access.flatMap: a =>
      HTTPRequest.userAgent(req).map(_.value) match
        case Some(UaUserRegex(u)) if a.me.isnt(UserStr(u)) => Left(UserAgentMismatch)
        case _                                             => Right(a)

  private val bearerSigner = Algo.hmac(mobileSecret.value)
  private def getTokenFromSignedBearer(full: Bearer): Fu[Option[AccessToken.ForAuth]] =
    val (bearer, signed) = full.value.split(':') match
      case Array(bearer, signed) if bearerSigner.sha1(bearer).hash_=(signed) => (Bearer(bearer), true)
      case _                                                                 => (full, false)
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

  type FetchUser[U]    = UserId => Fu[Option[U]]
  type AccessResult[U] = Either[AuthError, OAuthScope.Access[U]]
  type AuthResult[U]   = Either[AuthError, OAuthScope.Scoped[U]]

  sealed abstract class AuthError(val message: String) extends lila.core.lilaism.LilaException
  case object MissingAuthorizationHeader               extends AuthError("Missing authorization header")
  case object NoSuchToken                              extends AuthError("No such token")
  case class MissingScope(scopes: TokenScopes)         extends AuthError("Missing scope")
  case object NoSuchUser                               extends AuthError("No such user")
  case object OneUserWithTwoTokens extends AuthError("Both tokens belong to the same user")
  case object OriginBlocked        extends AuthError("Origin blocked")
  case object UserAgentMismatch extends AuthError("The user in the user-agent doesn't match the token bearer")

  def responseHeaders(accepted: EndpointScopes, tokenScopes: TokenScopes)(res: Result): Result =
    res.withHeaders(
      "X-OAuth-Scopes"          -> tokenScopes.into(OAuthScopes).keyList,
      "X-Accepted-OAuth-Scopes" -> accepted.into(OAuthScopes).keyList
    )
