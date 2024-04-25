package views.html.oAuth

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.oauth.AuthorizationRequest

object token:

  private lazy val ui = lila.oauth.ui.TokenUi(helpers)

  def create(form: Form[lila.oauth.OAuthTokenForm.Data], me: User)(using PageContext) =
    views.html.account.layout(title = trans.oauthScope.newAccessToken.txt(), active = "oauth.token"):
      ui.create(form, me)

  def index(tokens: List[lila.oauth.AccessToken])(using PageContext) =
    views.html.account.layout(title = trans.oauthScope.personalAccessTokens.txt(), active = "oauth.token"):
      ui.index(tokens)

object authorize:

  private lazy val ui = lila.oauth.ui.AuthorizeUi(helpers)(assetUrl, lightUserFallback)

  def apply(prompt: AuthorizationRequest.Prompt, me: User, authorizeUrl: String)(using PageContext) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth"),
      moreJs = embedJsUnsafe(ui.moreJs(prompt)),
      csp = defaultCsp.withLegacyCompatibility.some
    )(ui(prompt, me, authorizeUrl))
