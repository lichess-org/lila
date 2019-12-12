package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.richText
import lila.team.Team

import controllers.routes

object show {

  def apply(t: Team, members: Paginator[lila.team.MemberWithUser], info: lila.app.mashup.TeamInfo)(implicit ctx: Context) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui.OpenGraph(
        title = s"${t.name} team",
        url = s"$netBaseUrl${routes.Team.show(t.id).url}",
        description = shorten(t.description, 152)
      ).some
    )(
        main(cls := "page-menu")(
          bits.menu(none),
          div(cls := "team-show page-menu__content box team-show")(
            div(cls := "box__top")(
              h1(cls := "text", dataIcon := "f")(t.name, " ", em("TEAM")),
              div(
                if (t.disabled) span(cls := "staff")("CLOSED")
                else trans.nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize))
              )
            ),
            (info.mine || t.enabled) option div(cls := "team-show__content")(
              st.section(cls := "team-show__meta")(
                p(trans.teamLeader(), ": ", userIdLink(t.createdBy.some))
              ),

              div(cls := "team-show__members")(
                st.section(cls := "recent-members")(
                  h2(trans.teamRecentMembers()),
                  div(cls := "userlist infinitescroll")(
                    pagerNext(members, np => routes.Team.show(t.id, np).url),
                    members.currentPageResults.map { member =>
                      div(cls := "paginated")(userLink(member.user))
                    }
                  )
                )
              ),
              st.section(cls := "team-show__desc")(
                richText(t.description),
                t.location.map { loc =>
                  frag(br, trans.location(), ": ", richText(loc))
                },
                info.hasRequests option div(cls := "requests")(
                  h2(info.requests.size, " join requests"),
                  views.html.team.request.list(info.requests, t.some)
                )
              ),
              st.section(cls := "team-show__actions")(
                (t.enabled && !info.mine) option frag(
                  if (info.requestedByMe) strong("Your join request is being reviewed by the team leader")
                  else ctx.me.??(_.canTeam) option joinButton(t)
                ),
                (info.mine && !info.createdByMe) option
                  postForm(cls := "quit", action := routes.Team.quit(t.id))(
                    submitButton(cls := "button button-empty button-red confirm")(trans.quitTeam.txt())
                  ),
                (info.createdByMe || isGranted(_.Admin)) option
                  a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := "%")(trans.settings()),
                info.createdByMe option
                  a(href := routes.Tournament.teamBattleForm(t.id), cls := "button button-empty text", dataIcon := "g")("Team Battle")
              ),
              div(cls := "team-show__tour-forum")(
                info.teamBattles.nonEmpty option frag(
                  st.section(cls := "team-show__tour")(
                    h2(dataIcon := "g", cls := "text")(
                      trans.tournaments()
                    ),
                    views.html.tournament.teamBattle.list(info.teamBattles)
                  )
                ),
                NotForKids {
                  st.section(cls := "team-show__forum")(
                    h2(dataIcon := "d", cls := "text")(
                      a(href := teamForumUrl(t.id))(trans.forum()),
                      " (", info.forumNbPosts, ")"
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
                }
              )
            )
          )
        )
      )

  // handle special teams here
  private def joinButton(t: Team)(implicit ctx: Context) = t.id match {
    case "english-chess-players" => joinAt("https://ecf.chessvariants.training/")
    case "ecf" => joinAt(routes.Team.show("english-chess-players").url)
    case _ => postForm(cls := "inline", action := routes.Team.join(t.id))(
      submitButton(cls := "button button-green")(trans.joinTeam())
    )
  }

  private def joinAt(url: String)(implicit ctx: Context) =
    a(cls := "button button-green", href := url)(trans.joinTeam())
}
