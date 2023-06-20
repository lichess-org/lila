package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import controllers.routes

object notification {
  import bits.*
  import trans.preferences.*

  def apply(form: play.api.data.Form[?])(using WebContext) =
    account.layout(
      title = s"${trans.preferences.notifications.txt()} - ${preferences.txt()}",
      active = "notification"
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.preferences.notifications()),
        postForm(cls := "autosubmit", action := routes.Pref.notifyFormApply)(
          div(
            table(cls := "allows")(
              thead(
                tr(
                  th,
                  th(notifyBell(), iconTag(licon.BellOutline)),
                  th(notifyPush(), iconTag(licon.PhoneMobile))
                )
              ),
              tbody(
                List(
                  a(href := routes.Streamer.index())(notifyStreamStart()) -> "streamStart",
                  notifyForumMention()                                    -> "mention",
                  notifyInvitedStudy()                                    -> "invitedStudy",
                  notifyInboxMsg()                                        -> "privateMessage",
                  notifyChallenge()                                       -> "challenge",
                  notifyTournamentSoon()                                  -> "tournamentSoon",
                  notifyGameEvent()                                       -> "gameEvent"
                ).map(makeRow(form))
              )
            ),
            setting(
              correspondenceEmailNotification(),
              radios(form("correspondenceEmail"), translatedBooleanChoices)
            ),
            setting(
              bellNotificationSound(),
              radios(form("notification.playBellSound"), translatedBooleanIntChoices)
            )
          ),
          p(cls := "saved text none", dataIcon := licon.Checkmark)(yourPreferencesHaveBeenSaved())
        )
      )
    }

  private def makeRow(form: play.api.data.Form[_])(transFrag: Frag, filterName: String) =
    tr(
      td(transFrag),
      Seq("bell", "push") map { allow =>
        val name    = s"$filterName.$allow"
        val checked = form.data(name).contains("true")
        td(
          if (!hiddenFields(s"$filterName.$allow"))
            div(cls := "toggle", form3.cmnToggle(name, name, checked))
          else if (!checked)
            div(iconTag(licon.X))
          else
            div(
              cls := "always-on",
              form3.hidden(name, "true"),
              filterName match
                case "challenge"      => iconTag(licon.Swords)
                case "privateMessage" => iconTag(licon.BellOutline)
                case _                => emptyFrag
            )
        )
      }
    )

  private val hiddenFields = Set(
    "privateMessage.bell",
    "tournamentSoon.bell",
    "gameEvent.bell",
    "challenge.bell"
  )
}
