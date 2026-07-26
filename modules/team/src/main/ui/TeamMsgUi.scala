package lila.team
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class TeamMsgUi(helpers: Helpers)(using NetDomain):

  import helpers.{ *, given }

  def recent(msgs: TeamMsg.Recent, byTeam: TeamMsg.ByTeams, selected: Option[Team])(using Context) =
    Page("Team messages").css("bits.team.msg"):
      main(cls := "box team-msg")(
        div(cls := "team-msg__side")(
          div(cls := "team-msg__side__top"),
          div(cls := "team-msg__side__content"):
            byTeam.map: b =>
              import b.*
              val isSelected = selected.exists(_.id == team.id)
              a(
                href := routes.Team.messagesOf(team.id),
                cls := List(
                  "team-msg__side__team" -> true,
                  "team-msg__side__team--active" -> isSelected,
                  "team-msg__side__team--unread" -> (unread > 0)
                )
              )(
                span(cls := "team-msg__side__team__icon")(
                  teamFlair(team) | iconTag(lila.ui.Icon.Group)
                ),
                span(cls := "team-msg__side__team__content")(
                  span(cls := "team-msg__side__team__name")(team.name),
                  span(cls := "team-msg__side__team__meta")(
                    momentFromNowOnce(last),
                    Option.when(unread > 0 && !isSelected)(span(cls := "unread-count", unread))
                  )
                )
              )
        ),
        div(cls := "team-msg__convo")(
          div(cls := "team-msg__convo__head")(
            selected match
              case Some(team) =>
                frag(
                  button(cls := "team-msg__convo__head__back", dataIcon := Icon.LessThan),
                  teamLink(team.light, withIcon = false)
                )
              case None => h1("Team messages")
          ),
          div(cls := "team-msg__convo__msgs")(
            msgs.map: m =>
              import m.*
              div(cls := List("team-msg__convo__msg" -> true, "team-msg__convo__msg--unread" -> !seen))(
                div(cls := "team-msg__convo__msg__body")(richText(msg.text)),
                div(cls := "team-msg__convo__msg__info")(
                  teamLink(msg.team, withIcon = false),
                  div(cls := "team-msg__convo__msg__meta")(
                    span(trans.site.by(userIdLink(msg.senderId.some))),
                    momentFromNowOnce(msg.date)
                  )
                )
              )
          )
        )
      )
