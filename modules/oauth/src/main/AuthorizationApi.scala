package lila.oauth

import reactivemongo.api.bson.*

import lila.db.dsl.*
import io.mola.galimatias.URL

final class AuthorizationApi(val coll: Coll)(using Executor):
  import AuthorizationApi.{ BSONFields as F, PendingAuthorization, PendingAuthorizationBSONHandler }

  def create(request: AuthorizationRequest.Authorized): Fu[Protocol.AuthorizationCode] =
    val code = Protocol.AuthorizationCode.random()
    coll.insert
      .one(
        PendingAuthorizationBSONHandler.write:
          PendingAuthorization(
            code.hashed,
            request.clientId,
            request.user,
            request.redirectUri,
            request.challenge,
            request.scopes,
            nowInstant.plusSeconds(120)
          )
      )
      .inject(code)

  def consume(
      request: AccessTokenRequest.Prepared
  ): Fu[Either[Protocol.Error, AccessTokenRequest.Granted]] =
    coll.findAndModify($doc(F.hashedCode -> request.code.hashed), coll.removeModifier).map { doc =>
      for
        pending <- doc
          .result[PendingAuthorization]
          .toRight(Protocol.Error.AuthorizationCodeInvalid)
          .ensure(Protocol.Error.AuthorizationCodeExpired)(_.expires.isAfter(nowInstant))
          .ensure(Protocol.Error.MismatchingRedirectUri(request.redirectUri.value)):
            _.redirectUri.matches(request.redirectUri)
          .ensure(Protocol.Error.MismatchingClient(request.clientId))(_.clientId == request.clientId)
        _ <- pending.challenge match
          case Left(hashedClientSecret) =>
            request.clientSecret
              .toRight(LegacyClientApi.ClientSecretIgnored)
              .ensure(LegacyClientApi.MismatchingClientSecret)(_.matches(hashedClientSecret))
          case Right(codeChallenge) =>
            request.codeVerifier
              .toRight(LegacyClientApi.CodeVerifierIgnored)
              .ensure(Protocol.Error.MismatchingCodeVerifier)(_.matches(codeChallenge))
      yield AccessTokenRequest.Granted(pending.userId, pending.scopes.into(TokenScopes), pending.redirectUri)
    }

private object AuthorizationApi:
  object BSONFields:
    val hashedCode = "_id"
    val clientId = "clientId"
    val userId = "userId"
    val redirectUri = "redirectUri"
    val codeChallenge = "codeChallenge"
    val hashedClientSecret = "hashedClientSecret"
    val scopes = "scopes"
    val expires = "expires"

  case class PendingAuthorization(
      hashedCode: String,
      clientId: Protocol.ClientId,
      userId: UserId,
      redirectUri: Protocol.RedirectUri,
      challenge: Either[LegacyClientApi.HashedClientSecret, Protocol.CodeChallenge],
      scopes: OAuthScopes,
      expires: Instant
  )

  import lila.db.BSON
  import lila.db.dsl.{ *, given }
  import AuthorizationApi.BSONFields as F

  given PendingAuthorizationBSONHandler: BSON[PendingAuthorization] = new:
    def reads(r: BSON.Reader): PendingAuthorization =
      PendingAuthorization(
        hashedCode = r.str(F.hashedCode),
        clientId = Protocol.ClientId(r.str(F.clientId)),
        userId = r.get[UserId](F.userId),
        redirectUri = Protocol.RedirectUri(r.get[URL](F.redirectUri)),
        challenge = r.strO(F.hashedClientSecret) match
          case Some(hashedClientSecret) => Left(LegacyClientApi.HashedClientSecret(hashedClientSecret))
          case None => Right(Protocol.CodeChallenge(r.str(F.codeChallenge)))
        ,
        scopes = r.get[OAuthScopes](F.scopes),
        expires = r.get[Instant](F.expires)
      )

    def writes(w: BSON.Writer, o: PendingAuthorization) =
      $doc(
        F.hashedCode -> o.hashedCode,
        F.clientId -> o.clientId.value,
        F.userId -> o.userId,
        F.redirectUri -> o.redirectUri.value.toString,
        F.codeChallenge -> o.challenge.toOption,
        F.hashedClientSecret -> o.challenge.swap.toOption.map(_.value),
        F.scopes -> o.scopes,
        F.expires -> o.expires
      )
