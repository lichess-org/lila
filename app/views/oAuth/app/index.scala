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
            "Lichess now supports OAuth for unregistered and public clients with PKCE. ",
            "Here's a ",
            a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-app")(
              "Lichess OAuth app example"
            ),
            ", and the ",
            a(href := routes.Api.index)("API documentation"),
            ".",
            br,
            br,
            made.nonEmpty option {
              frag(
                flashMessage(cls := "flash-warning box__pad")(
                  "The following apps have been created while registration was still required. ",
                  "Please update them to use PKCE. ",
                  "Lichess will soon drop support for the authorization code flow without PKCE. ",
                  strong(a(href := "https://github.com/ornicar/lila/issues/9214")("More information")),
                  "."
                ),
                br,
                br
              )
            }
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
