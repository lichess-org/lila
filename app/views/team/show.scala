package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.richText
import lila.team.Team

import controllers.routes

object show {

  import trans.team._

  def apply(t: Team, members: Paginator[lila.team.MemberWithUser], info: lila.app.mashup.TeamInfo)(
      implicit ctx: Context
  ) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} team",
          url = s"$netBaseUrl${routes.Team.show(t.id).url}",
          description = shorten(t.description, 152)
        )
        .some
    )(
      main(cls := "page-menu")(
        bits.menu(none),
        div(cls := "team-show page-menu__content box team-show")(
          div(cls := "box__top")(
            h1(cls := "text", dataIcon := "f")(t.name, " ", em(trans.team.team.txt().toUpperCase)),
            div(
              if (t.disabled) span(cls := "staff")("CLOSED")
              else nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize))
            )
          ),
          (info.mine || t.enabled) option div(cls := "team-show__content")(
            st.section(cls := "team-show__meta")(
              p(teamLeader(), ": ", userIdLink(t.createdBy.some))
            ),
            div(cls := "team-show__members")(
              st.section(cls := "recent-members")(
                h2(teamRecentMembers()),
                div(cls := "userlist infinitescroll")(
                  pagerNext(members, np => routes.Team.show(t.id, np).url),
                  members.currentPageResults.map { member =>
                    div(cls := "paginated")(userLink(member.user))
                  }
                )
              )
            ),
            st.section(cls := "team-show__desc")(
              standardFlash(),
              richText(t.description),
              t.location.map { loc =>
                frag(br, trans.location(), ": ", richText(loc))
              },
              info.hasRequests option div(cls := "requests")(
                h2(xJoinRequests(info.requests.size)),
                views.html.team.request.list(info.requests, t.some)
              )
            ),
            st.section(cls := "team-show__actions")(
              (t.enabled && !info.mine) option frag(
                if (info.requestedByMe) strong(beingReviewed())
                else ctx.isAuth option joinButton(t)
              ),
              (info.mine && !info.createdByMe) option
                postForm(cls := "quit", action := routes.Team.quit(t.id))(
                  submitButton(cls := "button button-empty button-red confirm")(quitTeam.txt())
                ),
              (info.createdByMe || isGranted(_.Admin)) option
                a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := "%")(
                  trans.settings.settings()
                ),
              info.createdByMe option frag(
                a(
                  href := routes.Tournament.teamBattleForm(t.id),
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong(teamBattle()),
                    em("A battle of multiple teams, each players scores points for their team")
                  )
                ),
                a(
                  href := s"${routes.Tournament.form()}?team=${t.id}",
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong("Team tournament"),
                    em("An Arena tournament that only members of your team can join")
                  )
                ),
                a(
                  href := routes.Team.pmAll(t.id),
                  cls := "button button-empty text",
                  dataIcon := "e"
                )(
                  span(
                    strong("Message all members"),
                    em("Send a private message to every member of the team")
                  )
                )
              )
            ),
            div(cls := "team-show__tour-forum")(
              info.tournaments.nonEmpty option frag(
                st.section(cls := "team-show__tour")(
                  h2(dataIcon := "g", cls := "text")(trans.tournaments()),
                  info.tournaments.span(_.isCreated) match {
                    case (created, started) =>
                      frag(
                        views.html.tournament.bits.forTeam(created.sortBy(_.startsAt)),
                        views.html.tournament.bits.forTeam(started)
                      )
                  }
                )
              ),
              ctx.noKid option
                st.section(cls := "team-show__forum")(
                  h2(dataIcon := "d", cls := "text")(
                    a(href := teamForumUrl(t.id))(trans.forum()),
                    " (",
                    info.forumNbPosts,
                    ")"
                  ),
                  info.forumPosts.take(10).map { post =>
                    st.article(
                      p(cls := "meta")(
                        a(href := routes.ForumPost.redirect(post.postId))(post.topicName),
                        em(
                          userIdLink(post.userId, withOnline = false),
                          " ",
                          momentFromNow(post.createdAt)
                        )
                      ),
                      p(shorten(post.text, 200))
                    )
                  },
                  a(cls := "more", href := teamForumUrl(t.id))(t.name, " ", trans.forum(), " »")
                )
            )
          )
        )
      )
    )

  // handle special teams here
  private def joinButton(t: Team)(implicit ctx: Context) = t.id match {
    case "english-chess-players" => joinAt("https://ecf.octoknight.com/")
    case "ecf"                   => joinAt(routes.Team.show("english-chess-players").url)
    case _ =>
      postForm(cls := "inline", action := routes.Team.join(t.id))(
        submitButton(cls := "button button-green")(joinTeam())
      )
  }

  private def joinAt(url: String)(implicit ctx: Context) =
    a(cls := "button button-green", href := url)(joinTeam())
}
