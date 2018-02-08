package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lila.common.Iso
import lila.db.dsl._
import lila.user.{ User, UserRepo }

object OAuth {
  case class AccessTokenId(value: String) extends AnyVal
}

private final class OAuthServer(
    tokenColl: Coll,
    clientColl: Coll
) {

  import OAuth._

  private implicit val tokenIdHandler = stringAnyValHandler[AccessTokenId](_.value, AccessTokenId.apply)

  def activeUser(req: RequestHeader): Fu[Option[User]] = {
    req.headers get "Authorization" map (_.split(" ", 2))
  } ?? {
    case Array("Bearer", accessToken) => activeUser(AccessTokenId(accessToken))
    case _ => fuccess(none)
  }

  def activeUser(token: AccessTokenId): Fu[Option[User]] =
    tokenColl.primitiveOne[User.ID]($doc(
      "access_token_id" -> token,
      "expire_date" $gt DateTime.now
    ), "user_id") flatMap { _ ?? UserRepo.byId }
}
