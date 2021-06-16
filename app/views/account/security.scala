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
      thirdPartyApps: Boolean,
      personalAccessTokens: Boolean
  )(implicit
      ctx: Context
  ) =
    account.layout(title = s"${u.username} - ${trans.security.txt()}", active = "security") {
      div(cls := "account security")(
        div(cls := "box")(
          h1(trans.sessions()),
          standardFlash(cls := "box__pad"),
          div(cls := "box__pad")(
            p(trans.thisIsAListOfDevicesThatHaveLoggedIntoYourAccount()),
            sessions.sizeIs > 1 option div(
              trans.alternativelyYouCanX {
                postForm(cls := "revoke-all", action := routes.Account.signout("all"))(
                  submitButton(cls := "button button-empty button-red confirm")(
                    trans.revokeAllSessions()
                  )
                )
              }
            )
          ),
          table(sessions, curSessionId.some)
        ),
        thirdPartyApps option div(cls := "account security box")(
          h1("Third party apps"),
          p(cls := "box__pad")(
            "Revoke access of any ",
            a(href := routes.OAuthApp.index)("third party apps"),
            " that you do not trust."
          )
        ),
        personalAccessTokens option div(cls := "account security box")(
          h1("Personal access tokens"),
          p(cls := "box__pad")(
            "Revoke any ",
            a(href := routes.OAuthToken.index)("personal access tokens"),
            " that you do not recognize."
          )
        )
      )
    }

  def table(sessions: List[lila.security.LocatedSession], curSessionId: Option[String])(implicit lang: Lang) =
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
      }
    )
}
