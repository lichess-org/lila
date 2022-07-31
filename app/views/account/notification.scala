package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object notification {
  import bits._
  import trans.preferences._

  def fieldSet(form: play.api.data.Form[_], inactive: Boolean)(implicit ctx: Context) =
    div(cls := List("none" -> inactive))(
      setting(
        notifyInboxMsg(),
        radios(form("notification.inboxMsg"), translatedNotifyMoreChoices)
      ),
      setting(
        notifyForumMention(),
        radios(form("notification.forumMention"), translatedNotifyNoneMoreChoices)
      ),
      setting(
        notifyStreamStart(),
        radios(form("notification.streamStart"), translatedNotifyNoneMoreChoices)
      ),
      setting(
        notifyChallenge(),
        radios(form("notification.challenge"), translatedNotifyChoices)
      ),
      setting(
        notifyTournamentSoon(),
        radios(form("notification.tournamentSoon"), translatedNotifyChoices)
      ),
      setting(
        notifyGameEvent(),
        radios(form("notification.gameEvent"), translatedNotifyChoices)
      ),
      setting(
        notifyTimeAlarm(),
        radios(form("notification.timeAlarm"), translatedNotifyChoices)
      ),
      div(id := "correspondence-email-notif")(
        setting(
          correspondenceEmailNotification(),
          radios(form("notification.correspondenceEmail"), translatedBooleanIntChoices)
        )
      )
    )
}
