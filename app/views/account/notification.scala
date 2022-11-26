package views.html
package account

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object notification {
  import bits.*
  import trans.preferences.*

  def fieldSet(form: play.api.data.Form[_], inactive: Boolean)(using ctx: Context) =
    div(cls := List("none" -> inactive))(
      table(cls := "allows")(
        thead(
          tr(th, th(notifyBell()), th(notifyPush()))
        ),
        tbody(
          makeRow(form, notifyStreamStart(), "streamStart"),
          makeRow(form, notifyForumMention(), "mention"),
          makeRow(form, notifyInvitedStudy(), "invitedStudy"),
          makeRow(form, notifyInboxMsg(), "privateMessage"),
          makeRow(form, notifyChallenge(), "challenge"),
          makeRow(form, notifyTournamentSoon(), "tournamentSoon"),
          makeRow(form, notifyGameEvent(), "gameEvent")
        )
      ),
      setting(
        correspondenceEmailNotification(),
        radios(form("notification.correspondenceEmail"), translatedBooleanIntChoices)
      )
    )

  private def makeRow(form: play.api.data.Form[_], transTxt: Frag, filterName: String) =
    tr(
      td(transTxt),
      Seq("bell", "push") map { allow =>
        val name    = s"notification.$filterName.$allow"
        val checked = form.data(name).contains("true")
        td(
          if (editable(s"$filterName.$allow"))
            div(cls := "toggle", form3.cmnToggle(name, name, checked))
          else if (!checked)
            div(iconTag('\ue03f'))
          else
            div(
              cls := "always-on",
              form3.hidden(name, "true"),
              filterName match {
                case "challenge"      => iconTag('\ue048')
                case "privateMessage" => iconTag('\ue00f')
                case _                => emptyFrag
              }
            )
        )
      }
    )

  private def editable(name: String) = name match {
    case "privateMessage.bell" => false
    case "tournamentSoon.bell" => false
    case "gameEvent.bell"      => false
    case "challenge.bell"      => false
    case _                     => true
  }
}
