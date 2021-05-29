package lila.oauth

import org.joda.time.DateTime
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthServer(
    colls: OauthColls,
    userRepo: UserRepo,
    appApi: OAuthAppApi,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import AccessToken.accessTokenIdHandler
  import AccessToken.{ BSONFields => F }
  import OAuthServer._

  def auth(req: RequestHeader, scopes: List[OAuthScope]): Fu[AuthResult] =
    reqToTokenId(req).fold[Fu[AuthResult]](fufail(MissingAuthorizationHeader)) {
      auth(_, scopes)
    } recover { case e: AuthError =>
      Left(e)
    }

  def auth(tokenId: AccessToken.Id, scopes: List[OAuthScope]): Fu[AuthResult] =
    accessTokenCache.get(tokenId) orFailWith NoSuchToken flatMap {
      case at if scopes.nonEmpty && !scopes.exists(at.scopes.contains) => fufail(MissingScope(at.scopes))
      case at =>
        userRepo enabledById at.userId flatMap {
          case None    => fufail(NoSuchUser)
          case Some(u) => fuccess(OAuthScope.Scoped(u, at.scopes))
        }
    } dmap Right.apply recover { case e: AuthError =>
      Left(e)
    }

  def fetchAppAuthor(req: RequestHeader): Fu[Option[User.ID]] =
    reqToTokenId(req) ?? { tokenId =>
      colls.token {
        _.primitiveOne[OAuthApp.Id]($doc(F.id -> tokenId), F.clientId) flatMap {
          _ ?? appApi.authorOf
        }
      }
    }

  def authBoth(scopes: List[OAuthScope])(
      token1: AccessToken.Id,
      token2: AccessToken.Id
  ): Fu[Either[AuthError, (User, User)]] = for {
    auth1 <- auth(token1, scopes)
    auth2 <- auth(token2, scopes)
  } yield for {
    user1  <- auth1
    user2  <- auth2
    result <- if (user1.user is user2.user) Left(OneUserWithTwoTokens) else Right(user1.user -> user2.user)
  } yield result

  def deleteCached(id: AccessToken.Id): Unit =
    accessTokenCache.put(id, fuccess(none))

  private def reqToTokenId(req: RequestHeader): Option[AccessToken.Id] =
    req.headers.get(AUTHORIZATION).map(_.split(" ", 2)) collect { case Array("Bearer", tokenStr) =>
      AccessToken.Id(tokenStr)
    }

  private val accessTokenCache =
    cacheApi[AccessToken.Id, Option[AccessToken.ForAuth]](32, "oauth.server.personal_access_token") {
      _.expireAfterWrite(5 minutes)
        .buildAsyncFuture(fetchAccessToken)
    }

  private def fetchAccessToken(tokenId: AccessToken.Id): Fu[Option[AccessToken.ForAuth]] =
    colls.token {
      _.ext.findAndUpdate[AccessToken.ForAuth](
        selector = $doc(F.id -> tokenId),
        update = $set(F.usedAt -> DateTime.now),
        fields = AccessToken.forAuthProjection.some
      )
    }
}

object OAuthServer {

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object ServerOffline                            extends AuthError("OAuth server is offline! Try again soon.")
  case object MissingAuthorizationHeader               extends AuthError("Missing authorization header")
  case object InvalidAuthorizationHeader               extends AuthError("Invalid authorization header")
  case object NoSuchToken                              extends AuthError("No such token")
  case class MissingScope(scopes: List[OAuthScope])    extends AuthError("Missing scope")
  case object NoSuchUser                               extends AuthError("No such user")
  case object OneUserWithTwoTokens                     extends AuthError("Both tokens belong to the same user")

  def responseHeaders(acceptedScopes: Seq[OAuthScope], availableScopes: Seq[OAuthScope])(
      res: Result
  ): Result =
    res.withHeaders(
      "X-OAuth-Scopes"          -> OAuthScope.keyList(availableScopes),
      "X-Accepted-OAuth-Scopes" -> OAuthScope.keyList(acceptedScopes)
    )

  type Try = () => Fu[Option[OAuthServer]]
}
