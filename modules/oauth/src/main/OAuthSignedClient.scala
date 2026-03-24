package lila.oauth

import play.api.Configuration
import com.roundeights.hasher.Algo

import lila.oauth.Protocol.{ ClientId, RedirectUri }
import lila.common.config.given
import lila.core.net.{ Bearer, Origin, ValidReferrer }

case class OAuthSignedClient(
    clientId: ClientId,
    origins: List[Origin],
    scope: OAuthScope,
    signers: List[Algo.HmacBuilder],
    displayName: String
)
object OAuthSignedClient:
  case class SimpleSignup(username: UserName, email: EmailAddress)

final class OAuthSignedClients(appConfig: Configuration):

  private val config = appConfig.get[Configuration]("oauth.signedClients")
  private def signersOf(name: String) = config.get[List[String]](name + ".secrets").map(Algo.hmac)

  val mobile = OAuthSignedClient(
    ClientId("lichess_mobile"),
    List(Origin("org.lichess.mobile://")),
    OAuthScope.Web.Mobile,
    signersOf("mobile"),
    displayName = "Lichess Mobile"
  )

  val polygon = OAuthSignedClient(
    ClientId(config.get[String]("polygon.id")),
    Origin.from(List(config.get[String]("polygon.origin"), "http://localhost")),
    OAuthScope.Web.Polygon,
    signersOf("polygon"),
    displayName = config.get[String]("polygon.name")
  )

  def forPrompt(prompt: AuthorizationRequest.Prompt): Option[OAuthSignedClient] =
    forPrompt(prompt.clientId, prompt.redirectUri, prompt.scopes)

  def forPrompt(
      clientId: ClientId,
      redirectUri: RedirectUri,
      scopes: OAuthScopes
  ): Option[OAuthSignedClient] =
    clients.find: c =>
      clientId == c.clientId &&
        c.origins.has(redirectUri.origin) &&
        scopes.has(c.scope)

  def simpleSignupFrom(referrer: ValidReferrer): Option[OAuthSignedClient.SimpleSignup] =
    import lila.common.url.{ parse, queryParam }
    for
      ref <- parse(referrer.value).toOption
      username <- ref.queryParam("default_username").map(UserName(_))
      email <- ref.queryParam("default_email").flatMap(EmailAddress.from)
      sign <- ref.queryParam("default_sign")
      clientId <- ref.queryParam("client_id").map(ClientId(_))
      redirectUriStr <- ref.queryParam("redirect_uri")
      redirectUri <- RedirectUri.from(redirectUriStr).toOption
      scopes <- AuthorizationRequest.readScopes(~ref.queryParam("scope")).toOption
      client <- forPrompt(clientId, redirectUri, scopes)
      if client == polygon
      if client.signers.exists: signer =>
        signer.sha1(email.value).hash_=(sign)
    yield OAuthSignedClient.SimpleSignup(username, email)

  private val clients = List(mobile, polygon)

  private def forScopesOf(token: AccessToken.ForAuth): List[OAuthSignedClient] =
    clients.filter(c => token.scopes.value.contains(c.scope))

  /* Check that the token matching a provided bearer is allowed for use.
   * If the token matches a signed client, check that the signature is valid for that client.
   * If the token matches several signed clients, it will fail.
   * If it doesn't match any signed client, it will succeed without needing a signature. */
  def allow(bearer: Bearer, token: AccessToken.ForAuth, signature: Option[String]): Boolean =
    forScopesOf(token).forall: client =>
      token.clientOrigin.exists(client.origins.has) && signature.exists: signed =>
        client.signers.exists: signer =>
          signer.sha1(bearer.value).hash_=(signed)
