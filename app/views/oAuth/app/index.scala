package views.html.oAuth.app

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  def apply(made: List[lila.oauth.OAuthApp], used: List[lila.oauth.AccessToken.WithApp])(implicit
      ctx: Context
  ) =
    views.html.account.layout(title = "OAuth Apps", active = "oauth.app")(
      div(cls := "account oauth")(
        used.nonEmpty option div(cls := "oauth-used box")(
          h1(id := "used")("OAuth Apps"),
          standardFlash(cls := "box__pad"),
          table(cls := "slist slist-pad")(
            used.map { t =>
              tr(
                td(
                  strong(t.app.name),
                  " by ",
                  userIdLink(t.app.author.some),
                  br,
                  em(t.token.scopes.map(_.name).mkString(", "))
                ),
                td(cls := "date")(
                  a(href := t.app.homepageUri.toString)(t.app.homepageUri.toString),
                  br,
                  t.token.usedAt map { at =>
                    frag("Last used ", momentFromNow(at))
                  }
                ),
                td(cls := "action")(
                  postForm(action := routes.OAuthApp.revoke(t.token.id.value))(
                    submitButton(
                      cls := "button button-empty button-red confirm text",
                      title := s"Revoke access from ${t.app.name}",
                      dataIcon := ""
                    )("Revoke")
                  )
                )
              )
            }
          )
        ),
        div(cls := "oauth-made box")(
          h1(id := "made")("My OAuth Apps"),
          p(cls := "box__pad")(
            "Want to build something that integrates with and extends Lichess? ",
            a(href := routes.OAuthApp.create)("Register a new OAuth App"),
            " to get started developing with the Lichess API.",
            br,
            br,
            "Here's a ",
            a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-authorization-code")(
              "Lichess OAuth app example"
            ),
            ", and the ",
            a(href := routes.Api.index)("API documentation"),
            "."
          ),
          table(cls := "slist slist-pad")(
            made.map { t =>
              tr(
                td(
                  strong(t.name),
                  br,
                  t.description.map { em(_) }
                ),
                td(cls := "date")(
                  a(href := t.homepageUri.toString)(t.homepageUri.toString),
                  br,
                  "Created ",
                  momentFromNow(t.createdAt)
                ),
                td(cls := "action")(
                  a(
                    href := routes.OAuthApp.edit(t.clientId.value),
                    cls := "button button-empty",
                    title := "Edit this app",
                    dataIcon := ""
                  ),
                  postForm(action := routes.OAuthApp.delete(t.clientId.value))(
                    submitButton(
                      cls := "button button-empty button-red confirm",
                      title := "Delete this app",
                      dataIcon := ""
                    )
                  )
                )
              )
            }
          )
        )
      )
    )
}
