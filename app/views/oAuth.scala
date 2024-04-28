package views.oAuth

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.oauth.AuthorizationRequest

object token:

  private lazy val ui = lila.oauth.ui.TokenUi(helpers)

  def create(form: Form[lila.oauth.OAuthTokenForm.Data], me: User)(using PageContext) =
    views.account.layout(title = trans.oauthScope.newAccessToken.txt(), active = "oauth.token"):
      ui.create(form, me)

  def index(tokens: List[lila.oauth.AccessToken])(using PageContext) =
    views.account.layout(title = trans.oauthScope.personalAccessTokens.txt(), active = "oauth.token"):
      ui.index(tokens)

lazy val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)
