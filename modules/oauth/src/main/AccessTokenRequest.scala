package lila.oauth

import cats.data.Validated
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
      } yield Prepared(grantType, code, codeVerifier.some, clientId.some, redirectUri.some, None)

    def prepareLegacy: Validated[Error, Prepared] =
      for {
        grantType <- grantType.toValid(Error.GrantTypeRequired).andThen(GrantType.from)
        code      <- code.map(AuthorizationCode.apply).toValid(Error.CodeRequired)
        clientSecret <- clientSecret
          .map(LegacyClientApi.ClientSecret)
          .toValid(LegacyClientApi.ClientSecretRequired)
      } yield Prepared(grantType, code, None, clientId.map(ClientId.apply), None, clientSecret.some)
  }

  case class Prepared(
      grantType: GrantType,
      code: AuthorizationCode,
      codeVerifier: Option[CodeVerifier],
      clientId: Option[ClientId],
      redirectUri: Option[UncheckedRedirectUri],
      clientSecret: Option[LegacyClientApi.ClientSecret]
  )

  case class Granted(
      userId: User.ID,
      scopes: List[OAuthScope],
      redirectUri: RedirectUri
  )
}
