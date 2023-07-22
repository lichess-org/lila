package lila.oauth

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import lila.common.String.base64

object AccessTokenRequest:
  import Protocol.*

  case class Raw(
      grantType: Option[String],
      code: Option[String],
      codeVerifier: Option[String],
      clientId: Option[ClientId],
      redirectUri: Option[String],
      clientSecret: Option[String]
  ):
    def prepare: Either[Error, Prepared] =
      for
        _    <- grantType.toRight(Error.GrantTypeRequired).flatMap(GrantType.from)
        code <- code.map(AuthorizationCode.apply).toRight(Error.CodeRequired)
        codeVerifier <- codeVerifier
          .toRight(Protocol.Error.CodeVerifierRequired)
          .flatMap(Protocol.CodeVerifier.from)
        clientId    <- clientId.toRight(Error.ClientIdRequired)
        redirectUri <- redirectUri.map(UncheckedRedirectUri.apply).toRight(Error.RedirectUriRequired)
      yield Prepared(code, codeVerifier.some, clientId, redirectUri, None)

    def prepareLegacy(auth: Option[BasicAuth]): Either[Error, Prepared] =
      for
        _        <- grantType.toRight(Error.GrantTypeRequired).flatMap(GrantType.from)
        code     <- code.map(AuthorizationCode.apply).toRight(Error.CodeRequired)
        clientId <- clientId.orElse(auth.map(_.clientId)).toRight(Error.ClientIdRequired)
        clientSecret <- clientSecret
          .map(LegacyClientApi.ClientSecret.apply)
          .orElse(auth.map(_.clientSecret))
          .toRight(LegacyClientApi.ClientSecretRequired)
        redirectUri <- redirectUri.map(UncheckedRedirectUri.apply).toRight(Error.RedirectUriRequired)
      yield Prepared(code, None, clientId, redirectUri, clientSecret.some)

  case class Prepared(
      code: AuthorizationCode,
      codeVerifier: Option[CodeVerifier],
      clientId: ClientId,
      redirectUri: UncheckedRedirectUri,
      clientSecret: Option[LegacyClientApi.ClientSecret]
  )

  case class Granted(
      userId: UserId,
      scopes: TokenScopes,
      redirectUri: RedirectUri
  )

  case class BasicAuth(clientId: ClientId, clientSecret: LegacyClientApi.ClientSecret)
  object BasicAuth:
    def from(req: RequestHeader): Option[BasicAuth] =
      req.headers.get(HeaderNames.AUTHORIZATION).flatMap { authorization =>
        val prefix = "Basic "
        authorization.startsWith(prefix) option authorization.stripPrefix(prefix)
      } flatMap base64.decode flatMap {
        _.split(":", 2) match
          case Array(clientId, clientSecret) =>
            Some(BasicAuth(ClientId(clientId), LegacyClientApi.ClientSecret(clientSecret)))
          case _ => None
      }
