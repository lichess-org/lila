package lila.team
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.*

final class TeamMsgUi(helpers: Helpers):

  import helpers.*

  def recent(msgs: TeamMsg.Recent, byTeam: TeamMsg.ByTeam) =
    Page("Team messages").css("bits.team.msg"):
      main(cls := "box team-msg")(
        div(cls := "team-msg__side"):
          div(cls := "team-msg__side__content"):
            byTeam.map: b =>
              import b.*
              div(cls := "team-msg__side__team")(
                div(cls := "team-msg__side__team__icon")(
                  teamFlair(team) | iconTag(lila.ui.Icon.Group)
                ),
                div(cls := "team-msg__side__team__content")(
                  div(cls := "team-msg__side__team__name")(team.name),
                  div(cls := "team-msg__side__team__meta")(
                    momentFromNowOnce(last),
                    Option.when(unread > 0)(span(cls := "unread-count", unread))
                  )
                )
              )
        ,
        div(cls := "team-msg__convo")(msgs.toString)
      )
