package lila.team
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class TeamUpdateUi(helpers: Helpers)(using NetDomain):

  import helpers.{ *, given }
  import trans.team as trt

  def teamRecent(updates: TeamUpdate.Recent, byTeam: TeamUpdate.ByTeams, team: Team, subscribed: Boolean)(
      using Context
  ) =
    Page(team.name + " updates")
      .css("team.update")
      .js(esmInit("team.update"))
      .js(infiniteScrollEsmInit):
        main(cls := "box team-update team-update--team")(
          side(byTeam, team.id.some),
          div(cls := "team-update__convo")(
            div(cls := "team-update__convo__head")(
              div(cls := "team-update__convo__head__title")(
                a(href := routes.Team.updates())(
                  cls := "team-update__convo__head__back",
                  dataIcon := Icon.LessThan
                ),
                teamLink(team.light, withIcon = false)(cls := "team-link")
              ),
              div(cls := "team-update__convo__head__actions")(
                postForm(
                  cls := "team-update__convo__subscribe form3",
                  action := routes.Team.subscribe(team.id)
                )(
                  form3.cmnToggleWrap(
                    form3.cmnToggle("team-subscribe", "subscribe", checked = subscribed)(
                      title := trans.team.subToTeamUpdates.txt()
                    ),
                    span(cls := "toggle-text")(trans.team.subToTeamUpdates())
                  )
                )
              )
            ),
            msgList(updates)(routes.Team.updatesOf(team.id, _))
          )
        )

  def allRecent(msgs: TeamUpdate.Recent, byTeam: TeamUpdate.ByTeams)(using Context) =
    Page(trt.teamUpdates.txt())
      .css("team.update")
      .js(infiniteScrollEsmInit):
        main(cls := "box team-update team-update--all")(
          side(byTeam, selected = none),
          div(cls := "team-update__convo")(
            div(cls := "team-update__convo__head")(
              h1(dataIcon := Icon.InkQuill, cls := "text", trt.teamUpdates())
            ),
            msgList(msgs)(routes.Team.updates(_))
          )
        )

  def teamLatest(team: Team, msg: TeamUpdate[?, UserId])(using Context) =
    st.section(cls := "team-show__update")(
      h2(
        a(dataIcon := Icon.InkQuill, cls := "text", href := routes.Team.updatesOf(team.id))(trt.teamUpdates())
      ),
      div(cls := "team-show__update__last")(
        div(cls := "team-show__update__meta")(
          momentFromNowOnce(msg.date),
          span(trans.site.by(userIdLink(msg.sender.some)))
        ),
        div(cls := "team-show__update__body")(richText(msg.text, expandImg = false)),
        a(cls := "team-show__update__more", href := routes.Team.updatesOf(team.id))(trans.site.more(), " »")
      )
    )

  private def msgList(msgs: TeamUpdate.Recent)(nextUrl: Int => Call)(using Context) =
    div(cls := "team-update__convo__updates infinite-scroll", data("scroll-selector") := ".infinite-scroll")(
      msgs.currentPageResults.map: m =>
        import m.*
        div(
          cls := List(
            "team-update__convo__update paginated" -> true,
            "team-update__convo__update--unread" -> !seen
          )
        )(
          div(cls := "team-update__convo__update__info")(
            teamLink(msg.team, withIcon = false),
            div(cls := "team-update__convo__update__meta")(
              momentFromNowOnce(msg.date),
              span(trans.site.by(lightUserLink(msg.sender)))
            )
          ),
          div(cls := "team-update__convo__update__body")(richText(msg.text))
        )
      ,
      pagerNext(msgs, np => nextUrl(np).url)
    )

  private def side(byTeam: TeamUpdate.ByTeams, selected: Option[TeamId])(using Context) =
    div(cls := "team-update__side")(
      div(cls := "team-update__side__top")(
        a(href := routes.Team.mine, dataIcon := Icon.LessThan, cls := "team-update__side__back text")(
          trt.myTeams()
        )
      ),
      div(cls := "team-update__side__content"):
        byTeam.map: b =>
          import b.*
          val isSelected = selected.has(team.id)
          a(
            href := routes.Team.updatesOf(team.id),
            cls := List(
              "team-update__side__team" -> true,
              "team-update__side__team--active" -> isSelected,
              "team-update__side__team--unread" -> (unread > 0)
            )
          )(
            span(cls := "team-update__side__team__icon")(
              teamFlair(team) | iconTag(lila.ui.Icon.Group)
            ),
            span(cls := "team-update__side__team__content")(
              span(cls := "team-update__side__team__name")(team.name),
              span(cls := "team-update__side__team__meta")(
                momentFromNowOnce(last)(nbsp),
                Option.when(unread > 0 && !isSelected)(span(cls := "unread-count", unread))
              )
            )
          )
    )
