package lila.oauth

import org.joda.time.DateTime
import reactivemongo.api.bson._
import lila.db.dsl._

final class AuthorizationApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import AuthorizationApi.F

  def create(request: AuthorizationRequest.Authorized): Fu[AuthorizationRequest.Code] = {
    val code = AuthorizationRequest.Code.random()
    coll.insert.one($doc(
      F.hashed -> code.hashed,
      F.clientId -> request.clientId,
      F.redirectUri -> request.redirectUri,
      F.userId -> request.user,
      F.scopes -> request.scopes,
      F.expires -> DateTime.now().plusSeconds(120),
    )) inject code
  }

  def consume(code: AuthorizationRequest.Code): Fu[Option[Unit]] = {
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
