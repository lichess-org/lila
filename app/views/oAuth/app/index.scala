package views.html.oAuth.app

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  def apply(apps: List[lidraughts.oauth.OAuthApp])(implicit ctx: Context) =
    views.html.account.layout(title = "OAuth Apps", active = "oauth.app")(
      div(cls := "account oauth box")(
        div(cls := "box__top")(
          h1("OAuth Apps"),
          st.form(cls := "box__top__actions", action := routes.OAuthApp.create)(
            submitButton(cls := "button button-fat button-empty", title := "New app", dataIcon := "O")
          )
        ),
        p(cls := "box__pad")(
          "Want to build something that integrates with and extends Lidraughts? Register a new OAuth App to get started developing on the Lidraughts API.",
          br, br
        /*"Here's a ",
          a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-authorization-code")("lidraughts OAuth app example"),
          ", and the ", a(href := routes.Api.index)("API documentation"), "."*/
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
                postForm(action := routes.OAuthApp.delete(t.clientId.value))(
                  submitButton(cls := "button button-empty button-red confirm", title := "Delete this app", dataIcon := "q")
                )
              )
            )
          }
        )
      )
    )
}
