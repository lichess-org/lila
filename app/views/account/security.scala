package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import play.api.i18n.Lang

object security {

  def apply(
      u: lila.user.User,
      sessions: List[lila.security.LocatedSession],
      curSessionId: String,
      clients: List[lila.oauth.AccessTokenApi.Client],
      personalAccessTokens: Int
  )(implicit
      ctx: Context
  ) =
    account.layout(title = s"${u.username} - ${trans.security.txt()}", active = "security") {
      div(cls := "account security")(
        div(cls := "box")(
          h1(trans.security()),
          standardFlash(cls := "box__pad"),
          div(cls := "box__pad")(
            p(
              "This is a list of devices and applications that are logged into your account. If you notice any suspicious activity, make sure to ",
              a(href := routes.Account.email)("check your recovery email address"),
              " and ",
              a(href := routes.Account.passwd)("change your password"),
              "."
            ),
            sessions.sizeIs > 1 option div(
              "You can also ",
              postForm(cls := "revoke-all", action := routes.Account.signout("all"))(
                submitButton(cls := "button button-empty button-red confirm")(
                  trans.revokeAllSessions()
                )
              ),
              "."
            )
          ),
          table(sessions, curSessionId.some, clients, personalAccessTokens)
        )
      )
    }

  def table(
      sessions: List[lila.security.LocatedSession],
      curSessionId: Option[String],
      clients: List[lila.oauth.AccessTokenApi.Client],
      personalAccessTokens: Int
  )(implicit lang: Lang) =
    st.table(cls := "slist slist-pad")(
      sessions.map { s =>
        tr(
          td(cls := "icon")(
            span(
              cls := curSessionId.map { cur => s"is-${if (cur == s.session.id) "gold" else "green"}" },
              dataIcon := (if (s.session.isMobile) "" else "")
            )
          ),
          td(cls := "info")(
            span(cls := "ip")(s.session.ip.value),
            " ",
            span(cls := "location")(s.location.map(_.toString)),
            p(cls := "ua")(s.session.ua),
            s.session.date.map { date =>
              p(cls := "date")(
                momentFromNow(date),
                curSessionId has s.session.id option span(cls := "current")("[CURRENT]")
              )
            }
          ),
          curSessionId.map { cur =>
            td(
              s.session.id != cur option
                postForm(action := routes.Account.signout(s.session.id))(
                  submitButton(cls := "button button-red", title := trans.logOut.txt(), dataIcon := "")
                )
            )
          }
        )
      },
      clients map { client =>
        tr(
          td(cls := "icon")(span(cls := "is-green", dataIcon := "")),
          td(cls := "info")(
            strong(client.origin),
            p(cls := "ua")(
              if (client.scopes.nonEmpty)
                frag(
                  "Third party application with permissions: ",
                  client.scopes.map(_.name).mkString(", ")
                )
              else
                frag("Third party application using only public data.")
            ),
            client.usedAt map { usedAt =>
              p(cls := "date")(
                "Last used ",
                momentFromNow(usedAt)
              )
            }
          ),
          td(
            postForm(action := routes.OAuth.revokeClient)(
              input(tpe := "hidden", name := "origin", value := client.origin),
              submitButton(cls := "button button-red", title := "Revoke", dataIcon := "")
            )
          )
        )
      },
      (personalAccessTokens > 0) option tr(
        td(cls := "icon")(span(cls := "is-green", dataIcon := "")),
        td(cls := "info")(
          strong("Personal access tokens"),
          " can be used to access your account. Revoke any that you do not recognize."
        ),
        td(
          a(href := routes.OAuthToken.index, cls := "button", title := "API access tokens", dataIcon := "")
        )
      )
    )
}
