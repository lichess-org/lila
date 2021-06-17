package views.html
package oAuth
package app

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.user.User
import lila.oauth.AuthorizationRequest

object authorize {
  def apply(prompt: AuthorizationRequest.Prompt, me: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth")
    ) {
      main(cls := "oauth box box-pad")(
        h1("Authorize third party app"),
        postForm(
          p(
            strong(code(prompt.redirectUri.appOrigin)),
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
                  .replaceFirst(prompt.redirectUri.appOrigin, strong(prompt.redirectUri.appOrigin).render)
              )
            )
          ),
          form3.actions(
            a(href := prompt.cancelUrl)("Cancel"),
            submitButton(cls := "button")("Authorize ", me.username)
          )
        )
      )
    }
}
