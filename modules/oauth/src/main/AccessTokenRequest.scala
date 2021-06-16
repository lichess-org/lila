package lila.oauth

import cats.data.Validated
import lila.user.User

object AccessTokenRequest {
  import Protocol._

  case class Raw(
    grantType: Option[String],
    code: Option[String],
    redirectUri: Option[String],
    clientId: Option[String],
  ) {
    def prepare: Validated[Error, Prepared] =
      for {
        grantType <- grantType.toValid(Error.GrantTypeRequired).andThen(GrantType.from)
        code <- code.map(AuthorizationCode.apply).toValid(Error.CodeRequired)
        redirectUri <- redirectUri.toValid(Error.RedirectUriRequired).andThen(RedirectUri.from)
        clientId <- clientId.map(ClientId.apply).toValid(Error.ClientIdRequired)
      } yield Prepared(grantType, code, redirectUri, clientId)
  }

  case class Prepared(
    grantType: GrantType,
    code: AuthorizationCode,
    redirectUri: RedirectUri,
    clientId: ClientId,
  )

  case class Granted(
    userId: User.ID,
    scopes: List[OAuthScope],
    redirectUri: RedirectUri,
  )
}
