package lila.oauth

import play.api.mvc.RequestHeader
import play.api.{ Mode, Configuration }
import com.roundeights.hasher.Algo
import scalalib.net.Bearer

import lila.oauth.Protocol.{ ClientId, RedirectUri }
import lila.oauth.AuthorizationRequest.Prompt
import lila.common.config.given
import lila.core.config.BaseUrl
import lila.core.net.{ Origin, ValidReferrer }
import lila.core.misc.AuthCustomUi
import lila.ui.Context

case class OAuthSignedClient(
    clientId: ClientId,
    origins: List[Origin],
    scope: OAuthScope,
    signers: List[Algo.HmacBuilder],
    displayName: String,
    design: Option[AuthCustomUi] = None
)
object OAuthSignedClient:
  case class SimpleSignup(username: UserName, email: EmailAddress, client: OAuthSignedClient)
  type Action = "login" | "signup"

final class OAuthSignedClients(appConfig: Configuration, baseUrl: BaseUrl)(using mode: Mode)(using Executor):

  import OAuthSignedClient.Action

  private val config = appConfig.get[Configuration]("oauth.signedClients")
  private def signersOf(name: String) = config.get[List[String]](name + ".secrets").map(Algo.hmac)

  private val requireSign = mode.isProd || false // for easier dev

  val mobile = OAuthSignedClient(
    ClientId("lichess_mobile"),
    List(
      Origin("org.lichess.mobile://"),
      Origin(baseUrl.value)
    ),
    OAuthScope.Web.Mobile,
    signersOf("mobile"),
    displayName = "Lichess Mobile"
  )

  val takex3 = OAuthSignedClient(
    ClientId("takex3"),
    List(Origin("https://auth.taketaketake.com"), Origin("http://localhost")),
    OAuthScope.Web.Takex3,
    signersOf("takex3"),
    displayName = "Take Take Take",
    design = Some:
      AuthCustomUi(
        name = "Take Take Take",
        imagePath = "images/t3-logo.svg",
        cssClass = "takex3",
        lang = lila.core.i18n.enUsLang
      )
  )

  def monitor(client: Option[OAuthSignedClient], prompt: Prompt, action: Action)(using ctx: Context) =
    client.foreach: c =>
      monitoring.oauthAttempt(c.clientId, prompt, action, loggedIn = ctx.isAuth)

  def forReq(using RequestHeader): Either[String, (Prompt, Option[OAuthSignedClient])] =
    for
      prompt <- AuthorizationRequest.fromReq.left.map(_.description)
      withClient <-
        val signedClient = forPrompt(prompt.clientId, prompt.redirectUri, prompt.scopes)
        if areScopesAllowedFor(signedClient, prompt.scopes)
        then Right(prompt -> signedClient)
        else Left("Invalid scopes")
    yield withClient

  private def forPrompt(
      clientId: ClientId,
      redirectUri: RedirectUri,
      scopes: OAuthScopes
  ): Option[OAuthSignedClient] =
    clients.find: c =>
      clientId == c.clientId &&
        c.origins.has(redirectUri.origin) &&
        scopes.has(c.scope)

  private def areScopesAllowedFor(client: Option[OAuthSignedClient], scopes: OAuthScopes): Boolean =
    scopes.value.forall: scope =>
      client.exists(_.scope == scope) || !clients.exists(_.scope == scope)

  def simpleSignupFrom(referrer: ValidReferrer): Option[OAuthSignedClient.SimpleSignup] =
    import lila.common.url.{ parse, queryParam }
    for
      ref <- parse(referrer.value).toOption
      username <- ref.queryParam("default_username").map(UserName(_))
      email <- ref.queryParam("default_email").flatMap(EmailAddress.from)
      client <- signedReferrerClient(referrer)
    yield OAuthSignedClient.SimpleSignup(username, email, client)

  def isSignedReferrer(referrer: ValidReferrer): Boolean =
    signedReferrerClient(referrer).isDefined

  def signedReferrerClient(referrer: ValidReferrer): Option[OAuthSignedClient] =
    import lila.common.url.{ parse, queryParam }
    for
      ref <- parse(referrer.value).toOption
      email <- ref.queryParam("default_email").flatMap(EmailAddress.from)
      sign <- ref.queryParam("default_sign")
      clientId <- ClientId.from(ref.queryParam("client_id"))
      redirectUriStr <- ref.queryParam("redirect_uri")
      redirectUri <- RedirectUri.from(redirectUriStr).toOption
      scopes <- AuthorizationRequest.readScopes(~ref.queryParam("scope")).toOption
      client <- forPrompt(clientId, redirectUri, scopes)
      if client == takex3
      if !requireSign || client.signers.exists: signer =>
        signer.sha1(email.value).hash_=(sign)
    yield client

  private val clients = List(mobile, takex3)

  private def forScopesOf(token: AccessToken.ForAuth): List[OAuthSignedClient] =
    clients.filter(c => token.scopes.value.contains(c.scope))

  /* Check that the token matching a provided bearer is allowed for use.
   * If the token matches a signed client, check that the signature is valid for that client.
   * If the token matches several signed clients, it will fail.
   * If it doesn't match any signed client, it will succeed without needing a signature. */
  def allow(bearer: Bearer, token: AccessToken.ForAuth, signature: Option[String]): Boolean =
    forScopesOf(token).forall: client =>
      token.clientOrigin.exists(client.origins.has) && {
        !requireSign || signature.exists: signed =>
          client.signers.isEmpty || client.signers.exists: signer =>
            signer.sha1(bearer.value).hash_=(signed)
      }

  private object monitoring:
    private val newOauthAttempts = scalalib.cache.OnceEvery[(Prompt, Action)](10.minutes)
    def oauthAttempt(
        clientId: ClientId,
        prompt: Prompt,
        action: Action,
        loggedIn: Boolean
    ): Unit =
      if newOauthAttempts((prompt, action)) then
        lila.mon.signedClient.AuthPage(action).alreadyLoggedIn(clientId.value, loggedIn).increment()
