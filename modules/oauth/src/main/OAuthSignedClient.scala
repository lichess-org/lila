package lila.oauth

import play.api.{ Mode, Configuration }
import play.api.mvc.Call
import com.roundeights.hasher.Algo
import scalalib.net.Bearer

import lila.oauth.Protocol.{ ClientId, RedirectUri }
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
    design: Option[AuthCustomUi] = None,
    routes: Option[AuthCustomRoutes] = None
)
object OAuthSignedClient:
  case class SimpleSignup(username: UserName, email: EmailAddress, client: OAuthSignedClient)
  type Action = "login" | "signup"

case class AuthCustomRoutes(login: Call, signup: Call)

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
    design = AuthCustomUi(
      name = "Take Take Take",
      imagePath = "images/t3-logo.svg",
      cssClass = "takex3",
      lang = lila.core.i18n.enUsLang
    ).some,
    routes = AuthCustomRoutes(
      login = routes.Auth.loginTakex3,
      signup = routes.Auth.signupTakex3
    ).some
  )

  def forPromptAndMonitor(prompt: AuthorizationRequest.Prompt, action: Action)(using
      ctx: Context
  ): Option[OAuthSignedClient] =
    forPrompt(prompt.clientId, prompt.redirectUri, prompt.scopes).tap:
      _.foreach: c =>
        monitoring.oauthAttempt(c.clientId, prompt, action, loggedIn = ctx.isAuth)

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
    private val newOauthAttempts = scalalib.cache.OnceEvery[(AuthorizationRequest.Prompt, Action)](10.minutes)
    def oauthAttempt(
        clientId: ClientId,
        prompt: AuthorizationRequest.Prompt,
        action: Action,
        loggedIn: Boolean
    ): Unit =
      if newOauthAttempts((prompt, action)) then
        val monitor = if action == "signup" then lila.mon.signedClient.signup else lila.mon.signedClient.login
        monitor.alreadyLoggedIn(clientId.value, loggedIn).increment()
