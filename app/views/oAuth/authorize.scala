package views.html
package oAuth

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.user.User
import lila.oauth.AuthorizationRequest
import lila.oauth.OAuthScope

object authorize {

  val ringsImage = img(
    cls := "oauth__logo",
    alt := "linked rings icon",
    src := assetUrl("images/icons/linked-rings.png")
  )

  def footer(redirectUrl: String, isDanger: Boolean) = div(cls := "oauth__footer")(
    p(cls := List("danger" -> isDanger))("Not owned or operated by lichess.org"),
    p(cls := "oauth__redirect")(
      "Will redirect to ",
      redirectUrl
    )
  )

  def apply(prompt: AuthorizationRequest.Prompt, me: User, authorizeUrl: String)(implicit ctx: Context) = {
    val isDanger    = prompt.maybeScopes.exists(OAuthScope.dangerList.contains)
    val buttonClass = s"button${isDanger ?? " button-red confirm text"}"
    val buttonDelay = if (isDanger) 5000 else 2000
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth"),
      moreJs = embedJsUnsafe(
        // ensure maximum browser compatibility
        s"""setTimeout(function(){var el=document.getElementById('oauth-authorize');el.removeAttribute('disabled');el.setAttribute('class','$buttonClass')}, $buttonDelay);"""
      ),
      csp = defaultCsp.withLegacyCompatibility.some
    ) {
      main(cls := "oauth box box-pad")(
        div(cls := "oauth__top")(
          ringsImage,
          h1("Authorize"),
          strong(code(prompt.redirectUri.clientOrigin))
        ),
        prompt.redirectUri.insecure option flashMessage(cls := "flash-warning")(
          "Does not use a secure connection"
        ),
        postForm(action := authorizeUrl)(
          p(
            "Grant access to your ",
            strong(me.username),
            " account:"
          ),
          if (prompt.maybeScopes.isEmpty) ul(li("Only public data"))
          else
            ul(cls := "oauth__scopes")(
              prompt.maybeScopes map { scope =>
                li(
                  cls := List(
                    "danger" -> OAuthScope.dangerList(scope)
                  )
                )(scope.name)
              }
            ),
          form3.actions(
            a(href := prompt.cancelUrl)("Cancel"),
            submitButton(
              cls      := s"$buttonClass disabled",
              dataIcon := isDanger.option(""),
              disabled := true,
              id       := "oauth-authorize",
              title := s"The website ${prompt.redirectUri.host | prompt.redirectUri.withoutQuery} will get access to your Lichess account. Continue?"
            )("Authorize")
          ),
          footer(prompt.redirectUri.withoutQuery, isDanger)
        )
      )
    }
  }
}
