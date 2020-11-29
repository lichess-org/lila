package views.html.team

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.{ richText, safeJsonValue }
import lila.team.Team
import lila.app.mashup.TeamInfo

import controllers.routes

object show {

  import trans.team._

  def apply(
      t: Team,
      members: Paginator[lila.common.LightUser],
      info: TeamInfo,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: Option[lila.socket.Socket.SocketVersion]
  )(implicit
      ctx: Context
  ) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} team",
          url = s"$netBaseUrl${routes.Team.show(t.id).url}",
          description = shorten(t.description, 152)
        )
        .some,
      moreJs =
        for {
          v    <- socketVersion
          chat <- chatOption
        } yield frag(
          jsAt(s"compiled/lishogi.chat${isProd ?? ".min"}.js"),
          embedJsUnsafe(s"""lishogi.team=${safeJsonValue(
            Json.obj(
              "id"            -> t.id,
              "socketVersion" -> v.value,
              "chat" -> views.html.chat.json(
                chat.chat,
                name = if (t.isChatFor(_.LEADERS)) leadersChat.txt() else trans.chatRoom.txt(),
                timeout = chat.timeout,
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"team/${chat.chat.id}"),
                localMod = ctx.userId exists t.leaders.contains
              )
            )
          )}""")
        )
    )(
      main(
        cls := "team-show box",
        socketVersion.map { v =>
          data("socket-version") := v.value
        }
      )(
        div(cls := "box__top")(
          h1(cls := "text", dataIcon := "f")(t.name),
          div(
            if (t.disabled) span(cls := "staff")("CLOSED")
            else nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize))
          )
        ),
        (info.mine || t.enabled) option div(cls := "team-show__content")(
          div(cls := "team-show__content__col1")(
            st.section(cls := "team-show__meta")(
              p(
                teamLeaders.pluralSame(t.leaders.size),
                ": ",
                fragList(t.leaders.toList.map { l =>
                  userIdLink(l.some)
                })
              )
            ),
            chatOption.isDefined option frag(
              views.html.chat.frag,
              div(
                cls := "chat__members",
                aria.live := "off",
                aria.relevant := "additions removals text"
              )(
                span(cls := "number")(nbsp),
                " ",
                trans.spectators.txt().replace(":", ""),
                " ",
                span(cls := "list")
              )
            ),
            div(cls := "team-show__actions")(
              (t.enabled && !info.mine) option frag(
                if (info.requestedByMe) strong(beingReviewed())
                else ctx.isAuth option joinButton(t)
              ),
              (info.mine && !info.ledByMe) option
                postForm(cls := "quit", action := routes.Team.quit(t.id))(
                  submitButton(cls := "button button-empty button-red confirm")(quitTeam.txt())
                ),
              info.ledByMe option frag(
                a(
                  href := routes.Tournament.teamBattleForm(t.id),
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong(teamBattle()),
                    em(teamBattleOverview())
                  )
                ),
                a(
                  href := s"${routes.Tournament.form()}?team=${t.id}",
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong(teamTournament()),
                    em(teamTournamentOverview())
                  )
                ),
                a(
                  href := routes.Team.pmAll(t.id),
                  cls := "button button-empty text",
                  dataIcon := "e"
                )(
                  span(
                    strong(messageAllMembers()),
                    em(messageAllMembersOverview())
                  )
                )
              ),
              (info.ledByMe || isGranted(_.Admin)) option
                a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := "%")(
                  trans.settings.settings()
                )
            ),
            div(cls := "team-show__members")(
              st.section(cls := "recent-members")(
                h2(teamRecentMembers()),
                div(cls := "userlist infinitescroll")(
                  pagerNext(members, np => routes.Team.show(t.id, np).url),
                  members.currentPageResults.map { member =>
                    div(cls := "paginated")(lightUserLink(member))
                  }
                )
              )
            )
          ),
          div(cls := "team-show__content__col2")(
            standardFlash(),
            st.section(cls := "team-show__desc")(
              richText(t.description),
              t.location.map { loc =>
                frag(br, trans.location(), ": ", richText(loc))
              }
            ),
            info.hasRequests option div(cls := "team-show__requests")(
              h2(xJoinRequests.pluralSame(info.requests.size)),
              views.html.team.request.list(info.requests, t.some)
            ),
            div(cls := "team-show__tour-forum")(
              info.tours.nonEmpty option frag(
                st.section(cls := "team-show__tour team-tournaments")(
                  h2(a(href := routes.Team.tournaments(t.id))(trans.tournaments())),
                  table(cls := "slist")(
                    tournaments.renderList(
                      info.tours.next ::: info.tours.past.take(5 - info.tours.next.size)
                    )
                  )
                )
              ),
              ctx.noKid option
                st.section(cls := "team-show__forum")(
                  h2(a(href := teamForumUrl(t.id))(trans.forum())),
                  info.forumPosts.take(10).map { post =>
                    a(cls := "team-show__forum__post", href := routes.ForumPost.redirect(post.postId))(
                      div(cls := "meta")(
                        strong(post.topicName),
                        em(
                          post.userId map usernameOrId,
                          " • ",
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
  private def joinButton(t: Team)(implicit ctx: Context) =
    t.id match {
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
