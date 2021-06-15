package views.html
package oAuth
package app

import lila.oauth.AuthenticationRequest

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object authorize {
  def apply(prompt: AuthenticationRequest.Prompt)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Authorization",
      moreCss = cssTag("oauth")
    ) {
      main(cls := "oauth box box-pad")(
        h1(
          "Authorize ",
          prompt.humanReadableOrigin
        ),
        postForm(
          p(strong(prompt.humanReadableOrigin), " wants to access your Lichess account:"),
          if (prompt.scopes.isEmpty) ul(li("Only public data"))
          else
            ul(
              prompt.scopes map { scope =>
                li(scope.name)
              }
            ),
          flashMessage(cls := "flash-warning")(
            strong(prompt.humanReadableOrigin),
            " is not owned or operated by lichess.org! This form will redirect to ",
            strong(prompt.redirectUri.toString),
            "."
          ),
          form3.actions(
            a(href := prompt.cancelHref)("Back to ", prompt.humanReadableOrigin),
            submitButton(
              cls := "button"
            )("Authorize ", prompt.humanReadableOrigin)
          )
        )
      )
    }
}
