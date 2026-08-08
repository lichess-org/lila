package lila.team
package ui

import play.api.libs.json.{ Json, JsObject }
import scalalib.paginator.Paginator

import lila.common.Json.given
import lila.core.socket.SocketVersion
import lila.team.Team

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class TeamShowUi(helpers: Helpers, teamUi: TeamUi, requestUi: TeamRequestUi, updateUi: TeamUpdateUi):

  import helpers.{ *, given }
  import trans.team as trt

  def apply(
      s: Team.TeamShow,
      members: Paginator[lila.core.LightUser],
      chatOption: Option[(JsObject, SocketVersion, Frag)],
      toursFrag: Option[Frag],
      forumFrag: Frag,
      asMod: Boolean = false,
      modLog: List[Frag] = Nil
  )(using ctx: Context) =
    import s.*
    teamUi
      .TeamPage(team.name)
      .graph(
        title = s"${team.name} team",
        url = routeUrl(routes.Team.show(team.id)),
        description = team.intro.so { shorten(_, 152) }
      )
      .js(
        PageModule(
          "team",
          Json
            .obj("id" -> team.id)
            .add("chat" -> chatOption.map(_._1))
            .add("socketVersion" -> chatOption.map(_._2))
        )
      )
      .flag(_.noRobots, !team.enabled):
        val canManage = asMod && Granter.opt(_.ManageTeam)
        val canSeeMembers = canManage || (team.enabled && (team.publicMembers || mine))
        main(cls := "team-show box")(
          boxTop(
            h1(cls := "text", dataIcon := Icon.Group)(team.name, teamFlair(team.light)),
            div:
              if team.disabled then span(cls := "staff")("CLOSED")
              else
                canSeeMembers.option(a(href := routes.Team.members(team.slug)):
                  trt.nbMembers.plural(team.nbMembers, strong(team.nbMembers.localize)))
          ),
          div(cls := "team-show__content")(
            div(cls := "team-show__content__col1")(
              (team.enabled || ledByMe || canManage).option(
                st.section(cls := "team-show__meta")(
                  publicLeaders.nonEmpty.option(
                    p(
                      trt.teamLeaders.pluralSame(publicLeaders.size),
                      ": ",
                      publicLeaders.map(l => userIdLink(l.some))
                    )
                  ),
                  ledByMe.option(
                    a(
                      dataIcon := Icon.InfoCircle,
                      href := routes.Cms.lonePage(lila.core.id.CmsPageKey("team-etiquette")),
                      cls := "text"
                    )("Team Etiquette")
                  )
                )
              ),
              chatOption.map(_._3),
              teamUi.actions(team, member, myRequest, asMod),
              (canSeeMembers && !team.isClas).option(teamUi.members(team, members))
            ),
            div(cls := "team-show__content__col2")(
              standardFlash,
              (team.intro.isEmpty && havePerm(_.Settings)).option(
                div(cls := "flash flash-warning")(
                  div(cls := "flash__content"):
                    a(href := routes.Team.edit(team.id))("Give your team a short introduction text!")
                )
              ),
              modLog.nonEmpty.option:
                div(cls := "team-show__log")(
                  h2("Mod log"),
                  ul(modLog)
                )
              ,
              (team.enabled || canManage).option(
                st.section(cls := "team-show__desc")(
                  teamUi.markdown(team, team.descPrivate.ifTrue(mine || canManage) | team.description)
                )
              ),
              (team.enabled && requests.nonEmpty).option(
                div(cls := "team-show__requests")(
                  h2(trt.xJoinRequests.pluralSame(requests.size)),
                  requestUi.list(requests, team.some)
                )
              ),
              div(cls := "team-show__events")(
                team.enabled.so(update).map(updateUi.teamLatest(team, _)),
                (canSeeMembers && toursFrag.nonEmpty).option(
                  st.section(cls := "team-show__tour team-events team-tournaments")(
                    h2(
                      a(dataIcon := Icon.Trophy, cls := "text", href := routes.Team.tournaments(team.id))(
                        trans.site.tournaments()
                      )
                    ),
                    div(cls := "team-show__list-wrapper"):
                      table(cls := "slist slist-resp")(toursFrag)
                  )
                ),
                st.section(cls := "team-show__forum")(
                  h2(
                    a(dataIcon := Icon.BubbleConvo, cls := "text", href := teamForumUrl(team.id))(
                      trans.site.forum()
                    )
                  ),
                  div(cls := "team-show__list-wrapper")(forumFrag),
                  a(cls := "more", href := teamForumUrl(team.id))(
                    team.name,
                    " ",
                    trans.site.forum(),
                    " »"
                  )
                )
              )
            )
          )
        )
