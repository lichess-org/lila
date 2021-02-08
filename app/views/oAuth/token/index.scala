package views.html.oAuth.token

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  def apply(tokens: List[lila.oauth.AccessToken])(implicit ctx: Context) = {

    val title = "Personal API access tokens"

    views.html.account.layout(title = title, active = "oauth.token")(
      div(cls := "account oauth box")(
        div(cls := "box__top")(
          h1(title),
          st.form(cls := "box-top__actions", action := routes.OAuthToken.create)(
            submitButton(cls := "button frameless", st.title := "New access token", dataIcon := "O")
          )
        ),
        standardFlash(cls := "box__pad"),
        p(cls := "box__pad")(
          "You can make OAuth requests without going through the authorization code flow.",
          br,
          br,
          "Instead, ",
          a(href := routes.OAuthToken.create)("generate a personal token"),
          " that you can directly use in API requests.",
          br,
          br,
          "Be careful, these tokens are like passwords so you should guard them carefully. ",
          "The advantage to using a token over putting your password into a script is that a token can be revoked, ",
          "and you can generate lots of them.",
          br,
          br,
          "Here's a ",
          a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-personal-token")(
            "personal token app example"
          ),
          " and the ",
          a(href := routes.Api.index)("API documentation"),
          "."
        ),
        tokens.headOption.filter(_.isBrandNew).map { token =>
          div(cls := "box__pad brand")(
            iconTag("E")(cls := "is-green"),
            div(
              p(
                "Make sure to copy your new personal access token now.",
                br,
                "You won’t be able to see it again!"
              ),
              code(token.id.value)
            )
          )
        },
        table(cls := "slist slist-pad")(
          tokens.map { t =>
            tr(
              td(
                strong(t.description | "Unnamed"),
                br,
                em(t.scopes.map(_.name).mkString(", "))
              ),
              td(cls := "date")(
                t.createdAt.map { created =>
                  frag("Created ", momentFromNow(created), br)
                },
                t.usedAt.map { used =>
                  frag("Last used ", momentFromNow(used))
                }
              ),
              td(cls := "action")(
                postForm(action := routes.OAuthToken.delete(t.publicId.stringify))(
                  submitButton(
                    cls := "button button-red button-empty confirm",
                    st.title := "Delete this access token"
                  )("Delete")
                )
              )
            )
          }
        )
      )
    )
  }
}
