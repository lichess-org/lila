package lila.security

import org.joda.time.DateTime
import pdi.jwt.{ Jwt, JwtAlgorithm }
import play.api.mvc.RequestHeader
import play.api.libs.json.Json

import lila.common.Iso
import lila.db.dsl._
import lila.user.{ User, UserRepo }

object OAuth {
  case class AccessTokenJWT(value: String) extends AnyVal
  case class AccessTokenId(value: String) extends AnyVal
  object JWT {
    case class PublicKey(value: String) extends AnyVal
  }
}

private final class OAuthServer(
    tokenColl: Coll,
    clientColl: Coll,
    publicKey: OAuth.JWT.PublicKey
) {

  import OAuth._

  private implicit val tokenIdHandler = stringAnyValHandler[AccessTokenId](_.value, AccessTokenId.apply)

  def activeUser(req: RequestHeader): Fu[Option[User]] = {
    req.headers get "Authorization" map (_.split(" ", 2))
  } ?? {
    case Array("Bearer", token) => for {
      jsonStr <- Jwt.decodeRaw(token, publicKey.value, Seq(JwtAlgorithm.RS256)).pp.future
      json = Json.parse(jsonStr).pp
      accessToken = AccessTokenId((json str "jti" err s"Bad token json $json")).pp
      user <- activeUser(accessToken).thenPp
    } yield user
    case _ => fuccess(none)
  }

  def activeUser(token: AccessTokenId): Fu[Option[User]] =
    tokenColl.primitiveOne[User.ID]($doc(
      "access_token_id" -> token,
      "expire_date" $gt DateTime.now
    ), "user_id").thenPp flatMap { _ ?? UserRepo.byId }
}
