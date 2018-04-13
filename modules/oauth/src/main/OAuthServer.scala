package lila.oauth

import org.joda.time.DateTime
import pdi.jwt.{ Jwt, JwtAlgorithm }
import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }
import play.api.http.HeaderNames.AUTHORIZATION

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthServer(
    tokenColl: Coll,
    jwtPublicKey: JWT.PublicKey
) {

  import AccessToken.{ accessTokenIdHandler, ForAuth, ForAuthBSONReader }
  import AccessToken.{ BSONFields => F }
  import OAuthServer._

  def auth(req: RequestHeader, scopes: List[OAuthScope]): Fu[AuthResult] = {
    req.headers.get(AUTHORIZATION).map(_.split(" ", 2)) match {
      case Some(Array("Bearer", tokenStr)) => for {
        accessTokenId <- Jwt.decodeRaw(tokenStr, jwtPublicKey.value, Seq(JwtAlgorithm.RS256)).future map { jsonStr =>
          val json = Json.parse(jsonStr)
          AccessToken.Id((json str "jti" err s"Bad token json $json"))
        } recover {
          case _: Exception => AccessToken.Id(tokenStr) // personal access token
        }
        scoped <- tokenColl.uno[ForAuth]($doc(F.id -> accessTokenId)) flatMap {
          case None => fufail(NoSuchToken)
          case Some(at) if at.isExpired => fufail(ExpiredToken)
          case Some(at) if scopes.nonEmpty && !scopes.exists(at.scopes.contains) => fufail(MissingScope(at.scopes))
          case Some(at) =>
            setUsedNow(accessTokenId)
            UserRepo enabledById at.userId flatMap {
              case None => fufail(NoSuchUser)
              case Some(u) => fuccess(OAuthScope.Scoped(u, at.scopes))
            }
        }
      } yield Right(scoped)
      case Some(_) => fuccess(Left(InvalidAuthorizationHeader))
      case None => fuccess(Left(MissingAuthorizationHeader))
    }
  } recover {
    case e: AuthError => Left(e)
  }

  private def setUsedNow(tokenId: AccessToken.Id): Unit =
    tokenColl.updateFieldUnchecked($doc(F.id -> tokenId), F.usedAt, DateTime.now)
}

object OAuthServer {

  type AuthResult = Either[AuthError, OAuthScope.Scoped]

  sealed abstract class AuthError(val message: String) extends lila.base.LilaException
  case object ServerOffline extends AuthError("OAuth server is offline")
  case object MissingAuthorizationHeader extends AuthError("Missing authorization header")
  case object InvalidAuthorizationHeader extends AuthError("Invalid authorization header")
  case object NoSuchToken extends AuthError("No such token")
  case object ExpiredToken extends AuthError("Token has expired")
  case class MissingScope(scopes: List[OAuthScope]) extends AuthError("Missing scope")
  case object NoSuchUser extends AuthError("No such user")

  def responseHeaders(acceptedScopes: List[OAuthScope], availableScopes: List[OAuthScope])(res: Result): Result = res.withHeaders(
    "X-OAuth-Scopes" -> OAuthScope.keyList(availableScopes),
    "X-Accepted-OAuth-Scopes" -> OAuthScope.keyList(acceptedScopes)
  )

  type Try = () => Fu[Option[OAuthServer]]
}
