package lila.oauth

import org.joda.time.DateTime
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthServer(
    tokenColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import AccessToken.{ accessTokenIdHandler, ForAuth, ForAuthBSONReader }
  import AccessToken.{ BSONFields => F }
  import OAuthServer._

  def auth(req: RequestHeader, scopes: List[OAuthScope]): Fu[AuthResult] = {
    req.headers.get(AUTHORIZATION).map(_.split(" ", 2)) match {
      case Some(Array("Bearer", tokenStr)) =>
        val tokenId = AccessToken.Id(tokenStr)
        accessTokenCache.get(tokenId) flattenWith NoSuchToken flatMap {
          case at if scopes.nonEmpty && !scopes.exists(at.scopes.contains) => fufail(MissingScope(at.scopes))
          case at => UserRepo enabledById at.userId flatMap {
            case None => fufail(NoSuchUser)
            case Some(u) => fuccess(OAuthScope.Scoped(u, at.scopes))
          }
        } map Right.apply
      case Some(_) => fufail(InvalidAuthorizationHeader)
      case None => fufail(MissingAuthorizationHeader)
    }
  } recover {
    case e: AuthError => Left(e)
  }

  private val accessTokenCache = asyncCache.multi[AccessToken.Id, Option[AccessToken.ForAuth]](
    name = "oauth.server.personal_access_token",
    f = fetchAccessToken,
    expireAfter = _.ExpireAfterWrite(3 minutes)
  )

  private def fetchAccessToken(tokenId: AccessToken.Id): Fu[Option[AccessToken.ForAuth]] =
    tokenColl.findAndUpdate(
      selector = $doc(F.id -> tokenId),
      update = $set(F.usedAt -> DateTime.now),
      fields = AccessToken.forAuthProjection.some
    ).map(_.value) map2 AccessToken.ForAuthBSONReader.read
}

object OAuthServer {

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object ServerOffline extends AuthError("OAuth server is offline! Try again soon.")
  case object MissingAuthorizationHeader extends AuthError("Missing authorization header")
  case object InvalidAuthorizationHeader extends AuthError("Invalid authorization header")
  case object NoSuchToken extends AuthError("No such token")
  case class MissingScope(scopes: List[OAuthScope]) extends AuthError("Missing scope")
  case object NoSuchUser extends AuthError("No such user")

  def responseHeaders(acceptedScopes: List[OAuthScope], availableScopes: List[OAuthScope])(res: Result): Result = res.withHeaders(
    "X-OAuth-Scopes" -> OAuthScope.keyList(availableScopes),
    "X-Accepted-OAuth-Scopes" -> OAuthScope.keyList(acceptedScopes)
  )

  type Try = () => Fu[Option[OAuthServer]]
}
