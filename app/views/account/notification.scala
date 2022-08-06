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
      table( cls := "allows-grid")(
        thead(
          tr(th, th(notifyBell()), th(notifyPush()))
        ),
        tbody(
          makeRow(form, notifyStreamStart(),filterName="streamStart"),
          makeRow(form, notifyForumMention(), "forumMention"),
          makeRow(form, notifyInboxMsg(), "inboxMsg"),
          makeRow(form, notifyChallenge(), "challenge"),
          makeRow(form, notifyTournamentSoon(), filterName="tournamentSoon"),
          makeRow(form, notifyGameEvent(), "gameEvent"),
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
        val name = s"notification.$filterName.$allow"
        val checked = form.data(name).contains("true")
        td(
          if (editable(s"$filterName.$allow"))
            div(cls := "toggle", form3.cmnToggle(name, name, checked))
          else if (!checked)
            div(iconTag('\ue03f'))
          else
            div(
              cls := "always-on",
              form3.hidden(name, "true"), // force form value
              filterName match {
                case "challenge" => iconTag('\ue048')
                case "inboxMsg" => iconTag( '\ue00f')
                case _ => emptyFrag
              }
            )
        )
      }
    )

  private def editable(name: String) = name match {
    case "inboxMsg.bell" => false
    case "tournamentSoon.bell" => false
    case "gameEvent.bell" => false
    case "challenge.bell" => false
    case _ => true
  }
}
