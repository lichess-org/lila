package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object mod2 { // TODO: rename to mod

  def actions(u: User, emails: User.Emails, erased: User.Erased)(implicit ctx: Context) =
    div(id := "mz_actions")(
      isGranted(_.UserEvaluate) option div(cls := "btn-rack")(
        st.form(method := "POST", action := routes.Mod.spontaneousInquiry(u.username), title := "Start an inquiry")(
          button(cls := "btn-rack__btn inquiry", tpe := "submit")(i)
        ),
        st.form(method := "POST", action := routes.Mod.refreshUserAssess(u.username), title := "Collect data and analyze if the user is suspicious.")(
          button(cls := "btn-rack__btn", tpe := "submit")("Evaluate")
        ),
        isGranted(_.Shadowban) option {
          a(cls := "btn-rack__btn", href := routes.Mod.communicationPublic(u.id), title := "View communications")("Comms")
        },
        st.form(method := "POST", action := routes.Mod.notifySlack(u.id), title := "Notify slack #tavern", cls := "xhr")(
          button(cls := "btn-rack__btn", tpe := "submit")("Slack")
        )
      ),
      div(cls := "btn-rack")(
        isGranted(_.MarkEngine) option {
          st.form(method := "POST", action := routes.Mod.engine(u.username, !u.engine), title := "This user is clearly cheating.", cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.engine), tpe := "submit")("Engine")
          )
        },
        isGranted(_.MarkBooster) option {
          st.form(method := "POST", action := routes.Mod.booster(u.username, !u.booster), title := "Marks the user as a booster or sandbagger.", cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.booster), tpe := "submit")("Booster")
          )
        },
        isGranted(_.Shadowban) option {
          st.form(method := "POST", action := routes.Mod.troll(u.username, !u.troll), title := "Enable/disable communication features for this user.", cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.troll), tpe := "submit")("Shadowban")
          )
        },
        u.troll option {
          st.form(method := "POST", action := routes.Mod.deletePmsAndChats(u.username), title := "Delete all PMs and public chat messages", cls := "xhr")(
            button(cls := "btn-rack__btn confirm", tpe := "submit")("Clear PMs & chats")
          )
        },
        isGranted(_.RemoveRanking) option {
          st.form(method := "POST", action := routes.Mod.rankban(u.username, !u.rankban), title := "Include/exclude this user from the rankings.", cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.rankban), tpe := "submit")("Rankban")
          )
        },
        isGranted(_.ReportBan) option {
          st.form(method := "POST", action := routes.Mod.reportban(u.username, !u.reportban), title := "Enable/disable the report feature for this user.", cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.reportban), tpe := "submit")("Reportban")
          )
        }
      ),
      div(cls := "btn-rack")(
        isGranted(_.IpBan) option {
          st.form(method := "POST", action := routes.Mod.ban(u.username, !u.ipBan), cls := "xhr")(
            button(cls := List("btn-rack__btn" -> true, "active" -> u.ipBan), tpe := "submit")("IP ban")
          )
        },
        if (u.enabled) {
          isGranted(_.CloseAccount) option {
            st.form(method := "POST", action := routes.Mod.closeAccount(u.username), title := "Disables this account.", cls := "xhr")(
              button(cls := "btn-rack__btn", tpe := "submit")("Close")
            )
          }
        } else if (erased.value) {
          "Erased"
        } else {
          isGranted(_.ReopenAccount) option {
            st.form(method := "POST", action := routes.Mod.reopenAccount(u.username), title := "Re-activates this account.", cls := "xhr")(
              button(tpe := "submit", cls := "btn-rack__btn active")("Closed")
            )
          }
        }
      ),
      div(cls := "btn-rack")(
        (u.totpSecret.isDefined && isGranted(_.DisableTwoFactor)) option {
          st.form(method := "POST", action := routes.Mod.disableTwoFactor(u.username), title := "Disables two-factor authentication for this account.", cls := "xhr")(
            button(cls := "btn-rack__btn confirm", tpe := "submit")("Disable 2FA")
          )
        },
        isGranted(_.Impersonate) option {
          st.form(method := "post", action := routes.Mod.impersonate(u.username))(
            button(cls := "btn-rack__btn", tpe := "submit")("Impersonate")
          )
        }
      ),
      isGranted(_.SetTitle) option {
        st.form(cls := "fide_title", method := "POST", action := routes.Mod.setTitle(u.username))(
          form3.select(lila.user.DataForm.title.fill(u.title.map(_.value))("title"), lila.user.Title.all, "No title".some)
        )
      },
      isGranted(_.SetEmail) ?? frag(
        st.form(cls := "email", method := "POST", action := routes.Mod.setEmail(u.username))(
          st.input(tpe := "email", value := emails.current.??(_.value), name := "email", placeholder := "Email address"),
          button(tpe := "submit", cls := "button", dataIcon := "E")
        ),
        emails.previous.map { email =>
          s"Previously $email"
        }
      )
    )

  def userMarks(o: User, playbans: Option[Int])(implicit ctx: Context) = div(cls := "user_marks")(
    playbans.map { nb => iconTag("p", nb.toString)(title := "Playban") },
    o.troll option iconTag("c")(title := "Shadowban"),
    o.booster option iconTag("9")(title := "Boosting"),
    o.engine option iconTag("n")(title := "Engine"),
    o.ipBan option iconTag("2")(title := "IP ban", cls := "is-red"),
    o.disabled option iconTag("k")(title := "Closed"),
    o.reportban option iconTag("!")(title := "Reportban")
  )
}
