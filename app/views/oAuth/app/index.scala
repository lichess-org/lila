package views.html.oAuth.app

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  def apply(apps: List[lila.oauth.OAuthApp])(implicit ctx: Context) =
    views.html.account.layout(title = "OAuth Apps", active = "oauth.app")(
      div(cls := "account oauth box")(
        div(cls := "box__top")(
          h1("OAuth Apps"),
          st.form(cls := "box__top__actions", action := routes.OAuthApp.create)(
            button(tpe := "submit", cls := "button button-fat button-empty", title := "New app", dataIcon := "O")
          )
        ),
        p(cls := "box__pad")(
          "Want to build something that integrates with and extends Lichess? Register a new OAuth App to get started developing on the Lichess API.",
          br, br,
          "Here's a ",
          a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-authorization-code")("lichess OAuth app example"),
          ", and the ", a(href := routes.Api.index)("API documentation"), "."
        ),
        table(cls := "slist slist-pad")(
          apps.map { t =>
            tr(
              td(
                strong(t.name), br,
                t.description.map { em(_) }
              ),
              td(cls := "date")(
                t.homepageUri, br,
                "Created ", momentFromNow(t.createdAt)
              ),
              td(cls := "action")(
                a(href := routes.OAuthApp.edit(t.clientId.value), cls := "button", title := "Edit this app", dataIcon := "m"),
                st.form(action := routes.OAuthApp.delete(t.clientId.value), method := "POST")(
                  button(tpe := "submit", cls := "button button-empty button-red confirm", title := "Delete this app", dataIcon := "q")
                )
              )
            )
          }
        )
      )
    )
}
