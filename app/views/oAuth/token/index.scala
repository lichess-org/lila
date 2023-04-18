package views.html.oAuth.token

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object index:

  def apply(tokens: List[lila.oauth.AccessToken])(implicit ctx: Context) =

    val title = "Personal API access tokens"

    views.html.account.layout(title = title, active = "oauth.token")(
      div(cls := "account oauth box")(
        boxTop(
          h1(title),
          st.form(cls := "box-top__actions", action := routes.OAuthToken.create)(
            submitButton(cls := "button frameless", st.title := "New access token", dataIcon := "")
          )
        ),
        standardFlash.map(div(cls := "box__pad")(_)),
        p(cls := "box__pad force-ltr")(
          "You can make OAuth requests without going through the ",
          a(href := s"${routes.Api.index}#section/Introduction/Authentication")("authorization code flow"),
          ".",
          br,
          br,
          "Instead, ",
          a(href := routes.OAuthToken.create)("generate a personal access token"),
          " that you can directly use in API requests.",
          br,
          br,
          "Guard these tokens carefully! They are like passwords. ",
          "The advantage to using tokens over putting your password into a script is that tokens can be revoked, ",
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
            if (token.isDangerous) iconTag("")(cls := "is-red")
            else iconTag("")(cls                   := "is-green"),
            div(
              if (token.isDangerous)
                p(strong(trans.oauthScope.doNotShareIt()))
              else
                p(trans.oauthScope.copyTokenNow()),
              code(token.plain.value)
            )
          )
        },
        table(cls := "slist slist-pad")(
          tokens.map { t =>
            tr(
              td(
                strong(t.description | "Unnamed"),
                br,
                em(t.scopes.map(_.name.txt()).mkString(", "))
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
                postForm(action := routes.OAuthToken.delete(t.id.value))(
                  submitButton(
                    cls      := "button button-red button-empty confirm",
                    st.title := "Delete this access token"
                  )("Delete")
                )
              )
            )
          }
        )
      )
    )
