package views.html
package oAuth
package app

import lila.oauth.AuthorizationRequest

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object authorize {
  def apply(prompt: AuthorizationRequest.Prompt)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth")
    ) {
      main(cls := "oauth box box-pad")(
        h1(
          "Authorize ",
          prompt.redirectUri.appOrigin
        ),
        postForm(
          p(strong(prompt.redirectUri.appOrigin), " wants to access your Lichess account:"),
          if (prompt.maybeScopes.isEmpty) ul(li("Only public data"))
          else
            ul(
              prompt.maybeScopes map { scope =>
                li(scope.name)
              }
            ),
          flashMessage(cls := "flash-warning")(
            strong(prompt.redirectUri.appOrigin),
            " is not owned or operated by lichess.org! This form will redirect to ",
            strong(prompt.redirectUri.toString),
            "."
          ),
          form3.actions(
            a(href := prompt.cancelUrl)("Back to ", prompt.redirectUri.appOrigin),
            submitButton(
              cls := "button"
            )("Authorize ", prompt.redirectUri.appOrigin)
          )
        )
      )
    }
}
