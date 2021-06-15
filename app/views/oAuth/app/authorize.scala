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
      title = "Authorization"
    ) {
      main(cls := "box box-pad")(
        h1(
          "Authorize ", prompt.shortName
        ),
        form(method := "POST")(
          p(strong(prompt.shortName), " wants to access your Lichess account:"),
          if (prompt.scopes.isEmpty) ul(li("Only public data"))
          else ul(
            prompt.scopes map { scope =>
              li(scope.name)
            }
          ),
          a(href := prompt.cancel)("Back to ", prompt.shortName),
          submitButton(
            cls := "button"
          )("Authorize")
        )
      )
    }
}
