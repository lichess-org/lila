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
        code.hashed,
        request.clientId,
        request.user,
        request.redirectUri,
        request.challenge,
        request.scopes,
        DateTime.now().plusSeconds(120)
      )
    ) inject code
  }

  def consume(
      request: AccessTokenRequest.Prepared
  ): Fu[Validated[Protocol.Error, AccessTokenRequest.Granted]] =
    coll.findAndModify($doc(F.hashedCode -> request.code.hashed), coll.removeModifier) map { doc =>
      for {
        pending <- doc
          .result[PendingAuthorization]
          .toValid(Protocol.Error.AuthorizationCodeInvalid)
          .ensure(Protocol.Error.AuthorizationCodeExpired)(_.expires.isAfter(DateTime.now()))
          .ensure(Protocol.Error.MismatchingRedirectUri)(_.redirectUri.matches(request.redirectUri))
          .ensure(Protocol.Error.MismatchingClient)(_.clientId == request.clientId)
        _ <- pending.challenge match {
          case Left(hashedClientSecret) =>
            request.clientSecret
              .map(LegacyClientApi.ClientSecret)
              .toValid(LegacyClientApi.ClientSecretRequired)
              .ensure(LegacyClientApi.MismatchingClientSecret)(_.matches(hashedClientSecret))
              .map(_.unit)
          case Right(codeChallenge) =>
            request.codeVerifier
              .toValid(Protocol.Error.CodeVerifierRequired)
              .andThen(Protocol.CodeVerifier.from)
              .ensure(Protocol.Error.MismatchingCodeVerifier)(_.matches(codeChallenge))
              .map(_.unit)
        }
      } yield AccessTokenRequest.Granted(pending.userId, pending.scopes, pending.redirectUri)
    }
}

private object AuthorizationApi {
  object BSONFields {
    val hashedCode         = "_id"
    val clientId           = "clientId"
    val userId             = "userId"
    val redirectUri        = "redirectUri"
    val codeChallenge      = "codeChallenge"
    val hashedClientSecret = "hashedClientSecret"
    val scopes             = "scopes"
    val expires            = "expires"
  }

  case class PendingAuthorization(
      hashedCode: String,
      clientId: Protocol.ClientId,
      userId: User.ID,
      redirectUri: Protocol.RedirectUri,
      challenge: Either[LegacyClientApi.HashedClientSecret, Protocol.CodeChallenge],
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
        challenge = r.strO(F.hashedClientSecret) match {
          case Some(hashedClientSecret) => Left(LegacyClientApi.HashedClientSecret(hashedClientSecret))
          case None                     => Right(Protocol.CodeChallenge(r.str(F.codeChallenge)))
        },
        scopes = r.get[List[OAuthScope]](F.scopes),
        expires = r.get[DateTime](F.expires)
      )

    def writes(w: BSON.Writer, o: PendingAuthorization) =
      $doc(
        F.hashedCode         -> o.hashedCode,
        F.clientId           -> o.clientId.value,
        F.userId             -> o.userId,
        F.redirectUri        -> o.redirectUri.value.toString,
        F.codeChallenge      -> o.challenge.toOption.map(_.value),
        F.hashedClientSecret -> o.challenge.swap.toOption.map(_.value),
        F.scopes             -> o.scopes,
        F.expires            -> o.expires
      )
  }
}
