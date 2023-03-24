package views.html
package oAuth

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.oauth.AuthorizationRequest

object authorize {
  def apply(prompt: AuthorizationRequest.Prompt, me: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth"),
      moreJs = embedJsUnsafe(
        """setTimeout(function(){const el=document.getElementById('oauth-authorize');el.removeAttribute('disabled');}, 2000);"""
      )
    ) {
      main(cls := "oauth box box-pad")(
        h1(dataIcon := "a", cls := "text")("Authorize third party"),
        postForm(
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
            "Not owned or operated by lishogi.org"
          ),
          p(
            "This prompt will redirect to ",
            code(
              prompt.redirectUri.withoutQuery
            )
          ),
          form3.actions(
            a(href := prompt.cancelUrl)("Cancel"),
            submitButton(cls := "button", disabled := true, id := "oauth-authorize")("Authorize")
          )
        )
      )
    }
}
