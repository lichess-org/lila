package lila.oauth

import cats.data.Validated
import lila.user.User

object AccessTokenRequest {
  import Protocol._

  case class Raw(
      grantType: Option[String],
      code: Option[String],
      codeVerifier: Option[String],
      clientSecret: Option[String],
      redirectUri: Option[String],
      clientId: Option[String]
  ) {
    def prepare: Validated[Error, Prepared] =
      for {
        redirectUri <- redirectUri.map(UncheckedRedirectUri.apply).toValid(Error.RedirectUriRequired)
        clientId    <- clientId.map(ClientId.apply).toValid(Error.ClientIdRequired)
        grantType   <- grantType.toValid(Error.GrantTypeRequired).andThen(GrantType.from)
        code        <- code.map(AuthorizationCode.apply).toValid(Error.CodeRequired)
      } yield Prepared(grantType, code, codeVerifier, clientSecret, redirectUri, clientId)
  }

  case class Prepared(
      grantType: GrantType,
      code: AuthorizationCode,
      codeVerifier: Option[String],
      clientSecret: Option[String],
      redirectUri: UncheckedRedirectUri,
      clientId: ClientId
  )

  case class Granted(
      userId: User.ID,
      scopes: List[OAuthScope],
      redirectUri: RedirectUri
  )
}
