package lila.oauth

import org.joda.time.DateTime
import cats.data.Validated
import reactivemongo.api.bson._
import lila.db.dsl._
import lila.user.User

final class AuthorizationApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import AuthorizationApi.{ BSONFields => F, PendingAuthorization, PendingAuthorizationBSONHandler }

  def create(request: AuthorizationRequest.Authorized): Fu[Protocol.AuthorizationCode] = {
    val code = Protocol.AuthorizationCode.random()
    coll.insert.one(
      PendingAuthorizationBSONHandler write PendingAuthorization(
        code.secret.hashed,
        request.clientId,
        request.user,
        request.redirectUri,
        request.codeChallenge,
        request.scopes,
        DateTime.now().plusSeconds(120)
      )
    ) inject code
  }

  def consume(
      request: AccessTokenRequest.Prepared
  ): Fu[Validated[Protocol.Error, AccessTokenRequest.Granted]] =
    coll.findAndModify($doc(F.hashedCode -> request.code.secret.hashed), coll.removeModifier) map {
      _.result[PendingAuthorization]
        .toValid(Protocol.Error.AuthorizationCodeInvalid)
        .ensure(Protocol.Error.AuthorizationCodeExpired)(_.expires.isAfter(DateTime.now()))
        .ensure(Protocol.Error.MismatchingRedirectUri)(_.redirectUri.matches(request.redirectUri))
        .ensure(Protocol.Error.MismatchingClient)(_.clientId == request.clientId)
        .ensure(Protocol.Error.MismatchingCodeVerifier)(_.codeChallenge.matches(request.codeVerifier))
        .map { pending =>
          AccessTokenRequest.Granted(pending.userId, pending.scopes, pending.redirectUri)
        }
    }
}

private object AuthorizationApi {
  object BSONFields {
    val hashedCode    = "_id"
    val clientId      = "clientId"
    val userId        = "userId"
    val redirectUri   = "redirectUri"
    val codeChallenge = "codeChallenge"
    val scopes        = "scopes"
    val expires       = "expires"
  }

  case class PendingAuthorization(
      hashedCode: String,
      clientId: Protocol.ClientId,
      userId: User.ID,
      redirectUri: Protocol.RedirectUri,
      codeChallenge: Protocol.CodeChallenge,
      scopes: List[OAuthScope],
      expires: DateTime
  )

  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import AuthorizationApi.{ BSONFields => F }

  implicit object PendingAuthorizationBSONHandler extends BSON[PendingAuthorization] {
    def reads(r: BSON.Reader): PendingAuthorization =
      PendingAuthorization(
        hashedCode = r.str(F.hashedCode),
        clientId = Protocol.ClientId(r.str(F.clientId)),
        userId = r.str(F.userId),
        redirectUri = Protocol.RedirectUri.unchecked(r.str(F.redirectUri)),
        codeChallenge = Protocol.CodeChallenge(r.str(F.codeChallenge)),
        scopes = r.get[List[OAuthScope]](F.scopes),
        expires = r.get[DateTime](F.expires)
      )

    def writes(w: BSON.Writer, o: PendingAuthorization) =
      $doc(
        F.hashedCode    -> o.hashedCode,
        F.clientId      -> o.clientId.value,
        F.userId        -> o.userId,
        F.redirectUri   -> o.redirectUri.value.toString,
        F.codeChallenge -> o.codeChallenge.value,
        F.scopes        -> o.scopes,
        F.expires       -> o.expires
      )
  }
}
