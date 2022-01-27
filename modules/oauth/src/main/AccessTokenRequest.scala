package lila.oauth

import cats.data.Validated

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import lila.common.String.base64
import lila.user.User

object AccessTokenRequest {
  import Protocol._

  case class Raw(
      grantType: Option[String],
      code: Option[String],
      codeVerifier: Option[String],
      clientId: Option[String],
      redirectUri: Option[String],
      clientSecret: Option[String]
  ) {
    def prepare: Validated[Error, Prepared] =
      for {
        grantType <- grantType.toValid(Error.GrantTypeRequired).andThen(GrantType.from)
        code      <- code.map(AuthorizationCode.apply).toValid(Error.CodeRequired)
        codeVerifier <- codeVerifier
          .toValid(Protocol.Error.CodeVerifierRequired)
          .andThen(Protocol.CodeVerifier.from)
        clientId    <- clientId.map(ClientId.apply).toValid(Error.ClientIdRequired)
        redirectUri <- redirectUri.map(UncheckedRedirectUri.apply).toValid(Error.RedirectUriRequired)
      } yield Prepared(grantType, code, codeVerifier.some, clientId, redirectUri, None)

    def prepareLegacy(auth: Option[BasicAuth]): Validated[Error, Prepared] =
      for {
        grantType <- grantType.toValid(Error.GrantTypeRequired).andThen(GrantType.from)
        code      <- code.map(AuthorizationCode.apply).toValid(Error.CodeRequired)
        clientId  <- clientId.map(ClientId.apply).orElse(auth.map(_.clientId)).toValid(Error.ClientIdRequired)
        clientSecret <- clientSecret
          .map(LegacyClientApi.ClientSecret)
          .orElse(auth.map(_.clientSecret))
          .toValid(LegacyClientApi.ClientSecretRequired)
        redirectUri <- redirectUri.map(UncheckedRedirectUri.apply).toValid(Error.RedirectUriRequired)
      } yield Prepared(grantType, code, None, clientId, redirectUri, clientSecret.some)
  }

  case class Prepared(
      grantType: GrantType,
      code: AuthorizationCode,
      codeVerifier: Option[CodeVerifier],
      clientId: ClientId,
      redirectUri: UncheckedRedirectUri,
      clientSecret: Option[LegacyClientApi.ClientSecret]
  )

  case class Granted(
      userId: User.ID,
      scopes: List[OAuthScope],
      redirectUri: RedirectUri
  )

  case class BasicAuth(clientId: ClientId, clientSecret: LegacyClientApi.ClientSecret)
  object BasicAuth {
    def from(req: RequestHeader): Option[BasicAuth] =
      req.headers.get(HeaderNames.AUTHORIZATION).flatMap { authorization =>
        val prefix = "Basic "
        authorization.startsWith(prefix) option authorization.stripPrefix(prefix)
      } flatMap base64.decode flatMap {
        _.split(":", 2) match {
          case Array(clientId, clientSecret) =>
            Some(BasicAuth(ClientId(clientId), LegacyClientApi.ClientSecret(clientSecret)))
          case _ => None
        }
      }
  }
}
