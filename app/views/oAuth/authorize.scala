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
  def apply(prompt: AuthorizationRequest.Prompt, me: User, authorizeUrl: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth"),
      moreJs = embedJsUnsafeLoadThen(
        """setTimeout(() => {const el=document.getElementById('oauth-authorize');el.removeAttribute('disabled');el.classList.remove('disabled')}, 2000);"""
      )
    ) {
      main(cls := "oauth box box-pad")(
        h1(dataIcon := "", cls := "text")(prompt.redirectUri.clientOrigin),
        prompt.redirectUri.insecure option flashMessage(cls := "flash-warning")(
          "Does not use a secure connection"
        ),
        postForm(action := authorizeUrl)(
          p(
            strong(code(prompt.redirectUri.clientOrigin)),
            " wants to access your ",
            strong(me.username),
            " account:"
          ),
          if (prompt.maybeScopes.isEmpty) ul(li("Only public data"))
          else
            ul(cls := "oauth__scopes")(
              prompt.maybeScopes map { scope =>
                li(
                  cls := List(
                    "danger" -> (scope == OAuthScope.Web.Mod || scope == OAuthScope.Web.Login)
                  )
                )(scope.name)
              }
            ),
          form3.actions(
            a(href := prompt.cancelUrl)("Cancel"),
            submitButton(cls := "button disabled", disabled := true, id := "oauth-authorize")("Authorize")
          ),
          div(cls := "oauth__footer")(
            p(
              "Not owned or operated by lichess.org"
            ),
            p(cls := "oauth__redirect")(
              "Will redirect to ",
              raw(
                escapeHtmlRaw(prompt.redirectUri.value.toStringPunycode)
                  .replaceFirst(
                    prompt.redirectUri.clientOrigin,
                    strong(prompt.redirectUri.clientOrigin).render
                  )
              )
            )
          )
        )
      )
    }
}
