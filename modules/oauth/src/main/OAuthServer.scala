package lila.oauth

import play.api.mvc.{ RequestHeader, Result }

import lila.common.{ Bearer, HTTPRequest, Strings }
import lila.memo.SettingStore
import lila.user.{ User, UserRepo }

final class OAuthServer(
    tokenApi: AccessTokenApi,
    userRepo: UserRepo,
    originBlocklist: SettingStore[Strings]
)(using Executor):

  import OAuthServer.*

  def auth(req: RequestHeader, scopes: List[OAuthScope]): Fu[AuthResult] =
    HTTPRequest.bearer(req).fold[Fu[AuthResult]](fufail(MissingAuthorizationHeader)) {
      auth(_, scopes, req.some)
    } recover { case e: AuthError =>
      Left(e)
    }

  def auth(tokenId: Bearer, scopes: List[OAuthScope], andLogReq: Option[RequestHeader]): Fu[AuthResult] =
    tokenApi.get(tokenId) orFailWith NoSuchToken flatMap {
      case at if scopes.nonEmpty && !scopes.exists(at.scopes.contains) => fufail(MissingScope(at.scopes))
      case at =>
        userRepo enabledById at.userId flatMap {
          case None => fufail(NoSuchUser)
          case Some(u) =>
            val blocked =
              at.clientOrigin.exists(origin => originBlocklist.get().value.exists(origin.contains))
            andLogReq filter { req =>
              blocked || {
                u.id != User.explorerId && !HTTPRequest.looksLikeLichessBot(req)
              }
            } foreach { req =>
              logger.debug(
                s"${if (blocked) "block" else "auth"} ${at.clientOrigin | "-"} as ${u.username} ${HTTPRequest print req take 200}"
              )
            }
            if (blocked) fufail(OriginBlocked)
            else fuccess(OAuthScope.Scoped(u, at.scopes))
        }
    } dmap Right.apply recover { case e: AuthError =>
      Left(e)
    }

  def authBoth(scopes: List[OAuthScope], req: RequestHeader)(
      token1: Bearer,
      token2: Bearer
  ): Fu[Either[AuthError, (User, User)]] = for
    auth1 <- auth(token1, scopes, req.some)
    auth2 <- auth(token2, scopes, req.some)
  yield for
    user1  <- auth1
    user2  <- auth2
    result <- if (user1.user is user2.user) Left(OneUserWithTwoTokens) else Right(user1.user -> user2.user)
  yield result

object OAuthServer:

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object MissingAuthorizationHeader               extends AuthError("Missing authorization header")
  case object NoSuchToken                              extends AuthError("No such token")
  case class MissingScope(scopes: List[OAuthScope])    extends AuthError("Missing scope")
  case object NoSuchUser                               extends AuthError("No such user")
  case object OneUserWithTwoTokens extends AuthError("Both tokens belong to the same user")
  case object OriginBlocked        extends AuthError("Origin blocked")

  def responseHeaders(acceptedScopes: Seq[OAuthScope], availableScopes: Seq[OAuthScope])(
      res: Result
  ): Result =
    res.withHeaders(
      "X-OAuth-Scopes"          -> OAuthScope.keyList(availableScopes),
      "X-Accepted-OAuth-Scopes" -> OAuthScope.keyList(acceptedScopes)
    )
