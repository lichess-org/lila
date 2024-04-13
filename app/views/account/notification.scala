package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object notification:
  import bits.*
  import trans.preferences.*

  def apply(form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = s"${trans.preferences.notifications.txt()} - ${preferences.txt()}",
      active = "notification"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.preferences.notifications()),
        postForm(cls := "autosubmit", action := routes.Pref.notifyFormApply)(
          div(
            table(cls := "allows")(
              thead(
                tr(
                  th,
                  th(notifyBell(), iconTag(Icon.BellOutline)),
                  th(notifyPush(), iconTag(Icon.PhoneMobile))
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
                  frag("Broadcasts")                                      -> "broadcastRound",
                  notifyGameEvent()                                       -> "gameEvent"
                ).map(makeRow(form))
              )
            ),
            div(
              id := "correspondence-email-notif"
            )( // id is set to allow direct unsubcribe link in correspondence emails
              setting(
                correspondenceEmailNotification(),
                radios(form("correspondenceEmail"), translatedBooleanChoices)
              )
            ),
            setting(
              bellNotificationSound(),
              radios(form("notification.playBellSound"), translatedBooleanIntChoices)
            )
          ),
          p(cls := "saved text none", dataIcon := Icon.Checkmark)(yourPreferencesHaveBeenSaved())
        )
      )

  private def makeRow(form: play.api.data.Form[?])(transFrag: Frag, filterName: String) =
    tr(
      td(transFrag),
      Seq("bell", "push").map: allow =>
        val name    = s"$filterName.$allow"
        val checked = form.data(name).contains("true")
        td(
          if !hiddenFields(s"$filterName.$allow") then
            div(cls := "toggle", form3.cmnToggle(name, name, checked))
          else if !checked then div(iconTag(Icon.X))
          else
            div(
              cls := "always-on",
              form3.hidden(name, "true"),
              filterName match
                case "challenge"      => iconTag(Icon.Swords)
                case "privateMessage" => iconTag(Icon.BellOutline)
                case _                => emptyFrag
            )
        )
    )

  private val hiddenFields = Set(
    "privateMessage.bell",
    "tournamentSoon.bell",
    "gameEvent.bell",
    "challenge.bell"
  )
