package lila.oauth

import org.joda.time.DateTime
import pdi.jwt.{ Jwt, JwtAlgorithm }
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import lila.common.Iso
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthServer(
    tokenColl: Coll,
    clientColl: Coll,
    jwtPublicKey: JWT.PublicKey
) {

  private implicit val tokenIdHandler = stringAnyValHandler[AccessTokenId](_.value, AccessTokenId.apply)

  def activeUser(req: RequestHeader): Fu[Option[User]] = {
    req.headers get "Authorization" map (_.split(" ", 2))
  } ?? {
    case Array("Bearer", token) => for {
      jsonStr <- Jwt.decodeRaw(token, jwtPublicKey.value, Seq(JwtAlgorithm.RS256)).future
      json = Json.parse(jsonStr)
      accessToken = AccessTokenId((json str "jti" err s"Bad token json $json"))
      user <- activeUser(accessToken)
    } yield user
    case _ => fuccess(none)
  }

  def activeUser(token: AccessTokenId): Fu[Option[User]] =
    tokenColl.primitiveOne[User.ID]($doc(
      "access_token_id" -> token,
      "expire_date" $gt DateTime.now
    ), "user_id") flatMap { _ ?? UserRepo.byId }
}

object OAuthServer {

  type Try = () => Fu[Option[OAuthServer]]

  def appliesTo(req: RequestHeader) = req.headers.toMap contains "Authorization"
}
