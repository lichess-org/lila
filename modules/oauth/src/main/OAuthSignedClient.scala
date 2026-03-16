package lila.oauth

import play.api.Configuration

import lila.oauth.Protocol.ClientId
import lila.common.config.given
import lila.core.net.{ Bearer, Origin }
import com.roundeights.hasher.Algo

case class OAuthSignedClient(
    clientId: ClientId,
    origin: Origin,
    scope: OAuthScope,
    signers: List[Algo.HmacBuilder],
    displayName: String
)

final class OAuthSignedClients(appConfig: Configuration):

  private val config = appConfig.get[Configuration]("oauth.signedClients")
  private def signersOf(name: String) = config.get[List[String]](name + ".secrets").map(Algo.hmac)

  val mobile = OAuthSignedClient(
    ClientId("lichess_mobile"),
    Origin("org.lichess.mobile://"),
    OAuthScope.Web.Mobile,
    signersOf("mobile"),
    displayName = "Lichess Mobile"
  )

  val polygon = OAuthSignedClient(
    ClientId("polygon"),
    Origin(config.get[String]("polygon.origin")),
    OAuthScope.Web.Polygon,
    signersOf("polygon"),
    displayName = "Polygon"
  )

  def forPrompt(prompt: AuthorizationRequest.Prompt): Option[OAuthSignedClient] =
    clients.find: c =>
      prompt.clientId == c.clientId &&
        prompt.redirectUri.origin == c.origin &&
        prompt.scopes.has(c.scope)

  private val clients = List(mobile, polygon)

  private def forScopesOf(token: AccessToken.ForAuth): List[OAuthSignedClient] =
    clients.filter(c => token.scopes.value.contains(c.scope))

  /* Check that the token matching a provided bearer is allowed for use.
   * If the token matches a signed client, check that the signature is valid for that client.
   * If the token matches several signed clients, it will fail.
   * If it doesn't match any signed client, it will succeed without needing a signature. */
  def allow(bearer: Bearer, token: AccessToken.ForAuth, signature: Option[String]): Boolean =
    forScopesOf(token).forall: client =>
      token.clientOrigin.has(client.origin) && signature.exists: signed =>
        client.signers.exists: signer =>
          signer.sha1(bearer.value).hash_=(signed)
