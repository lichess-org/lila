package lila.oauth

import org.joda.time.DateTime
import cats.data.Validated
import reactivemongo.api.bson._
import lila.db.dsl._

final class AuthorizationApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import AuthorizationApi.F

  def create(request: AuthorizationRequest.Authorized): Fu[Protocol.AuthorizationCode] = {
    val code = Protocol.AuthorizationCode.random()
    coll.insert.one($doc(
      F.hashed -> code.secret.hashed,
      F.clientId -> request.clientId.value,
      F.redirectUri -> request.redirectUri.value.toString,
      F.userId -> request.user,
      F.scopes -> request.scopes,
      F.expires -> DateTime.now().plusSeconds(120),
    )) inject code
  }

  def consume(request: AccessTokenRequest.Prepared): Fu[Validated[Protocol.Error, AccessTokenRequest.Granted]] =
    coll.findAndModify($doc(F.hashed -> request.code.secret.hashed), coll.removeModifier) map { result =>
      ???
    }
}

object AuthorizationApi {
  private object F {
    val hashed = "_id"
    val clientId = "clientId"
    val userId = "userId"
    val redirectUri = "redirectUri"
    val codeChallenge = "codeChallenge"
    val scopes = "scopes"
    val expires = "expires"
  }
}
