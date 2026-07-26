package lila.team
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class TeamMsgUi(helpers: Helpers)(using NetDomain):

  import helpers.{ *, given }

  def teamRecent(msgs: TeamMsg.Recent, byTeam: TeamMsg.ByTeams, team: Team, subscribed: Boolean)(using
      Context
  ) =
    Page(team.name + " messages")
      .css("bits.team.msg")
      .js(Esm("bits.teamMsg")):
        main(cls := "box team-msg")(
          side(byTeam, team.id.some),
          div(cls := "team-msg__convo")(
            div(cls := "team-msg__convo__head")(
              div(cls := "team-msg__convo__head__title")(
                a(href := routes.Team.messages)(
                  cls := "team-msg__convo__head__back",
                  dataIcon := Icon.LessThan
                ),
                teamLink(team.light, withIcon = false)(cls := "team-link")
              ),
              div(
                postForm(
                  cls := "team-msg__convo__subscribe form3",
                  action := routes.Team.subscribe(team.id)
                )(
                  form3.cmnToggleWrap(
                    form3.cmnToggle("team-subscribe", "subscribe", checked = subscribed),
                    trans.team.subToTeamMessages()
                  )
                )
              )
            ),
            msgList(msgs)
          )
        )

  def allRecent(msgs: TeamMsg.Recent, byTeam: TeamMsg.ByTeams)(using Context) =
    Page("Team messages").css("bits.team.msg"):
      main(cls := "box team-msg")(
        side(byTeam, selected = none),
        div(cls := "team-msg__convo")(
          div(cls := "team-msg__convo__head")(
            h1("Team messages")
          ),
          msgList(msgs)
        )
      )

  private def msgList(msgs: TeamMsg.Recent)(using Context) =
    div(cls := "team-msg__convo__msgs")(
      msgs.map: m =>
        import m.*
        div(cls := List("team-msg__convo__msg" -> true, "team-msg__convo__msg--unread" -> !seen))(
          div(cls := "team-msg__convo__msg__body")(richText(msg.text)),
          div(cls := "team-msg__convo__msg__info")(
            teamLink(msg.team, withIcon = false),
            div(cls := "team-msg__convo__msg__meta")(
              momentFromNowOnce(msg.date),
              span(trans.site.by(userIdLink(msg.senderId.some)))
            )
          )
        )
    )

  private def side(byTeam: TeamMsg.ByTeams, selected: Option[TeamId]) =
    div(cls := "team-msg__side")(
      div(cls := "team-msg__side__top"),
      div(cls := "team-msg__side__content"):
        byTeam.map: b =>
          import b.*
          val isSelected = selected.has(team.id)
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
                momentFromNowOnce(last)(nbsp),
                Option.when(unread > 0 && !isSelected)(span(cls := "unread-count", unread))
              )
            )
          )
    )
