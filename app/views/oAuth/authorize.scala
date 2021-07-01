package views.html
package oAuth

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.user.User
import lila.oauth.AuthorizationRequest

object authorize {
  def apply(prompt: AuthorizationRequest.Prompt, me: User, authorizeUrl: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth"),
      moreJs = embedJsUnsafeLoadThen(
        """setTimeout(() => $('#oauth-authorize').removeAttr('disabled').removeClass('disabled'), 2000);"""
      )
    ) {
      main(cls := "oauth box box-pad")(
        h1(dataIcon := "", cls := "text")("Authorize third party"),
        postForm(action := authorizeUrl)(
          p(
            strong(code(prompt.redirectUri.clientOrigin)),
            " wants to access your ",
            strong(me.username),
            " account:"
          ),
          if (prompt.maybeScopes.isEmpty) ul(li("Only public data"))
          else
            ul(
              prompt.maybeScopes map { scope =>
                li(scope.name)
              }
            ),
          flashMessage(cls := "flash-warning")(
            "Not owned or operated by lichess.org"
          ),
          p(
            "This prompt will redirect to ",
            code(
              raw(
                escapeHtmlRaw(prompt.redirectUri.value.toStringPunycode)
                  .replaceFirst(
                    prompt.redirectUri.clientOrigin,
                    strong(prompt.redirectUri.clientOrigin).render
                  )
              )
            )
          ),
          form3.actions(
            a(href := prompt.cancelUrl)("Cancel"),
            submitButton(cls := "button disabled", disabled := true, id := "oauth-authorize")("Authorize")
          )
        ),
        prompt.maybeLegacy option p(
          "Note for developers: The OAuth flow without PKCE is ",
          a(href := "https://github.com/ornicar/lila/issues/9214")("deprecated"),
          ". Consider updating your app."
        )
      )
    }
}
