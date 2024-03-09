package lila.oauth

import play.api.mvc.{ RequestHeader, Result }

import lila.common.{ Bearer, HTTPRequest }
import lila.user.UserRepo

final class OAuthServer(
    tokenApi: AccessTokenApi,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import OAuthServer._

  def auth(req: RequestHeader, scopes: List[OAuthScope]): Fu[AuthResult] =
    HTTPRequest.bearer(req).fold[Fu[AuthResult]](fufail(MissingAuthorizationHeader)) {
      auth(_, scopes, req.some)
    } recover {
      case e: AuthError => Left(e)
    }

  def auth(tokenId: Bearer, scopes: List[OAuthScope], andLogReq: Option[RequestHeader]): Fu[AuthResult] =
    tokenApi.get(tokenId) orFailWith NoSuchToken flatMap {
      case at if scopes.nonEmpty && !scopes.exists(at.scopes.contains) => fufail(MissingScope(at.scopes))
      case at =>
        userRepo enabledById at.userId flatMap {
          case None => fufail(NoSuchUser)
          case Some(u) =>
            andLogReq foreach { req =>
              logger.debug(
                s"auth ${at.clientOrigin | "-"} as ${u.username} ${HTTPRequest print req take 200}"
              )
            }
            fuccess(OAuthScope.Scoped(u, at.scopes))
        }
    } dmap Right.apply recover {
      case e: AuthError => Left(e)
    }

}

object OAuthServer {

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object MissingAuthorizationHeader               extends AuthError("Missing authorization header")
  case object NoSuchToken                              extends AuthError("No such token")
  case class MissingScope(scopes: List[OAuthScope])    extends AuthError("Missing scope")
  case object NoSuchUser                               extends AuthError("No such user")

  def responseHeaders(acceptedScopes: Seq[OAuthScope], availableScopes: Seq[OAuthScope])(
      res: Result
  ): Result =
    res.withHeaders(
      "X-OAuth-Scopes"          -> OAuthScope.keyList(availableScopes),
      "X-Accepted-OAuth-Scopes" -> OAuthScope.keyList(acceptedScopes)
    )
}
