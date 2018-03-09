package lila.oauth

import org.joda.time.DateTime
import pdi.jwt.{ Jwt, JwtAlgorithm }
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthServer(
    tokenColl: Coll,
    clientColl: Coll,
    jwtPublicKey: JWT.PublicKey
) {

  import AccessToken.accessTokenIdHandler
  import AccessToken.{ BSONFields => F }

  def activeUser(req: RequestHeader): Fu[Option[User]] = {
    req.headers get "Authorization" map (_.split(" ", 2))
  } ?? {
    case Array("Bearer", token) => for {
      accessTokenId <- Jwt.decodeRaw(token, jwtPublicKey.value, Seq(JwtAlgorithm.RS256)).future map { jsonStr =>
        val json = Json.parse(jsonStr)
        AccessToken.Id((json str "jti" err s"Bad token json $json"))
      } recover {
        case _: Exception => AccessToken.Id(token) // personal access token
      }
      user <- activeUser(accessTokenId)
    } yield Some(user err "No user found for access token")
    case _ => fuccess(none)
  } mapFailure { e =>
    new OauthException { val message = e.getMessage }
  }

  def activeUser(tokenId: AccessToken.Id): Fu[Option[User]] =
    tokenColl.primitiveOne[User.ID]($doc(
      F.id -> tokenId,
      F.expiresAt $gt DateTime.now
    ), F.userId) flatMap {
      _ ?? { userId =>
        setUsedNow(tokenId)
        UserRepo byId userId
      }
    }

  private def setUsedNow(tokenId: AccessToken.Id): Unit =
    tokenColl.updateFieldUnchecked($doc(F.id -> tokenId), F.usedAt, DateTime.now)
}

object OAuthServer {

  type Try = () => Fu[Option[OAuthServer]]

  def appliesTo(req: RequestHeader) = req.headers.toMap contains "Authorization"
}
