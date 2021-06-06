package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import play.api.i18n.Lang

object security {

  def apply(u: lila.user.User, sessions: List[lila.security.LocatedSession], curSessionId: String)(implicit
      ctx: Context
  ) =
    account.layout(title = s"${u.username} - ${trans.security.txt()}", active = "security") {
      div(cls := "account security box")(
        h1(trans.security()),
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
