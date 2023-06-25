package lila.oauth

import com.softwaremill.tagging.*
import play.api.mvc.{ RequestHeader, Result }
import com.roundeights.hasher.Algo

import lila.common.{ Bearer, HTTPRequest, Strings }
import lila.memo.SettingStore
import lila.user.{ User, UserRepo }
import lila.common.config.Secret

final class OAuthServer(
    tokenApi: AccessTokenApi,
    userRepo: UserRepo,
    originBlocklist: SettingStore[Strings] @@ OriginBlocklist,
    mobileSecret: Secret @@ MobileSecret
)(using Executor):

  import OAuthServer.*

  def auth(req: RequestHeader, accepted: EndpointScopes): Fu[AuthResult] =
    HTTPRequest.bearer(req).fold[Fu[AuthResult]](fufail(MissingAuthorizationHeader)) {
      auth(_, accepted, req.some)
    } recover { case e: AuthError =>
      Left(e)
    } addEffect { res =>
      monitorAuth(res.isRight, req)
    }

  def auth(tokenId: Bearer, accepted: EndpointScopes, andLogReq: Option[RequestHeader]): Fu[AuthResult] =
    getTokenFromSignedBearer(tokenId) orFailWith NoSuchToken flatMap {
      case at if !accepted.isEmpty && !accepted.compatible(at.scopes) =>
        fufail(MissingScope(at.scopes))
      case at =>
        userRepo me at.userId flatMap {
          case None => fufail(NoSuchUser)
          case Some(u) =>
            val blocked =
              at.clientOrigin.exists(origin => originBlocklist.get().value.exists(origin.contains))
            andLogReq
              .filter: req =>
                blocked || {
                  u.userId != User.explorerId && !HTTPRequest.looksLikeLichessBot(req)
                }
              .foreach: req =>
                logger.debug:
                  s"${if (blocked) "block" else "auth"} ${at.clientOrigin | "-"} as ${u.username} ${HTTPRequest print req take 200}"
            if blocked then fufail(OriginBlocked)
            else fuccess(OAuthScope.Scoped(u, at.scopes))
        }
    } dmap Right.apply recover { case e: AuthError =>
      Left(e)
    }

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
      if user1.me is user2.me
      then Left(OneUserWithTwoTokens)
      else Right(user1.user -> user2.user)
  yield result

  private val bearerSigner = Algo hmac mobileSecret.value
  private def getTokenFromSignedBearer(full: Bearer): Fu[Option[AccessToken.ForAuth]] =
    val (bearer, signed) = full.value.split(':') match
      case Array(bearer, signed) if bearerSigner.sha1(bearer) hash_= signed => (Bearer(bearer), true)
      case _                                                                => (full, false)
    tokenApi
      .get(bearer)
      .mapz: token =>
        if token.scopes.has(_.Web.Mobile) && !signed then
          logger.warn(s"Web:Mobile token requested but not signed: $token")
          none
        else token.some

  private val cleanUaRegex = """ user:[\w\-]{2,30}""".r
  private def monitorAuth(success: Boolean, req: RequestHeader) =
    val ua      = HTTPRequest.userAgent(req).fold("none")(_.value)
    val cleanUa = cleanUaRegex.replaceAllIn(ua, "")
    lila.mon.user.oauth.request(cleanUa, success).increment()

object OAuthServer:

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object MissingAuthorizationHeader               extends AuthError("Missing authorization header")
  case object NoSuchToken                              extends AuthError("No such token")
  case class MissingScope(scopes: TokenScopes)         extends AuthError("Missing scope")
  case object NoSuchUser                               extends AuthError("No such user")
  case object OneUserWithTwoTokens extends AuthError("Both tokens belong to the same user")
  case object OriginBlocked        extends AuthError("Origin blocked")

  def responseHeaders(accepted: EndpointScopes, tokenScopes: TokenScopes)(res: Result): Result =
    res.withHeaders(
      "X-OAuth-Scopes"          -> tokenScopes.into(OAuthScopes).keyList,
      "X-Accepted-OAuth-Scopes" -> accepted.into(OAuthScopes).keyList
    )
