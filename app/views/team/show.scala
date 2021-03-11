package views.html.team

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.mashup.TeamInfo
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.{ markdownLinksOrRichText, richText, safeJsonValue }
import lila.team.Team

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
      moreJs = frag(
        jsModule("team"),
        embedJsUnsafeLoadThen(s"""teamStart(${safeJsonValue(
          Json
            .obj("id" -> t.id)
            .add("socketVersion" -> socketVersion.map(_.value))
            .add("chat" -> chatOption.map { chat =>
              views.html.chat.json(
                chat.chat,
                name = if (t.isChatFor(_.LEADERS)) leadersChat.txt() else trans.chatRoom.txt(),
                timeout = chat.timeout,
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"team/${chat.chat.id}"),
                localMod = ctx.userId exists t.leaders.contains
              )
            })
        )})""")
      )
    ) {
      val enabledOrLeader = t.enabled || info.ledByMe || isGranted(_.Admin)
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
        div(cls := "team-show__content")(
          div(cls := "team-show__content__col1")(
            enabledOrLeader option st.section(cls := "team-show__meta")(
              p(
                teamLeaders.pluralSame(t.leaders.size),
                ": ",
                fragList(t.leaders.toList.map { l =>
                  userIdLink(l.some)
                })
              ),
              info.ledByMe option a(
                dataIcon := "",
                href := routes.Page.loneBookmark("team-etiquette"),
                cls := "text team-show__meta__etiquette"
              )("Team Etiquette")
            ),
            (t.enabled && chatOption.isDefined) option frag(
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
                if (info.requestedByMe)
                  frag(
                    strong(beingReviewed()),
                    postForm(action := routes.Team.quit(t.id))(
                      submitButton(cls := "button button-red button-empty confirm")(trans.cancel())
                    )
                  )
                else ctx.isAuth option joinButton(t)
              ),
              ctx.userId.ifTrue(t.enabled && info.mine) map { myId =>
                postForm(
                  cls := "team-show__subscribe form3",
                  action := routes.Team.subscribe(t.id)
                )(
                  div(
                    span(form3.cmnToggle("team-subscribe", "subscribe", checked = info.subscribed)),
                    label(`for` := "team-subscribe")("Subscribe to team messages")
                  )
                )
              },
              (info.mine && !info.ledByMe) option
                postForm(cls := "quit", action := routes.Team.quit(t.id))(
                  submitButton(cls := "button button-empty button-red confirm")(quitTeam.txt())
                ),
              t.enabled && info.ledByMe option frag(
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
                  href := s"${routes.Tournament.form}?team=${t.id}",
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong(teamTournament()),
                    em(teamTournamentOverview())
                  )
                ),
                a(
                  href := s"${routes.Swiss.form(t.id)}",
                  cls := "button button-empty text",
                  dataIcon := "g"
                )(
                  span(
                    strong(trans.swiss.swissTournaments()),
                    em("A Swiss tournament that only members of your team can join")
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
              ((t.enabled && info.ledByMe) || isGranted(_.Admin)) option
                a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := "%")(
                  trans.settings.settings()
                )
            ),
            t.enabled option div(cls := "team-show__members")(
              st.section(cls := "recent-members")(
                h2(teamRecentMembers()),
                div(cls := "userlist infinite-scroll")(
                  members.currentPageResults.map { member =>
                    div(cls := "paginated")(lightUserLink(member))
                  },
                  pagerNext(members, np => routes.Team.show(t.id, np).url)
                )
              )
            )
          ),
          div(cls := "team-show__content__col2")(
            standardFlash(),
            st.section(cls := "team-show__desc")(
              markdownLinksOrRichText(t.description),
              if (info.mine) t.descPrivate.map { desc =>
                frag(br, markdownLinksOrRichText(desc))
              },
              t.location.map { loc =>
                frag(br, trans.location(), ": ", richText(loc))
              }
            ),
            t.enabled && info.hasRequests option div(cls := "team-show__requests")(
              h2(xJoinRequests.pluralSame(info.requests.size)),
              views.html.team.request.list(info.requests, t.some)
            ),
            div(
              t.enabled && info.simuls.nonEmpty option frag(
                st.section(cls := "team-show__tour team-events team-simuls")(
                  h2(trans.simultaneousExhibitions()),
                  views.html.simul.bits.allCreated(info.simuls)
                )
              ),
              t.enabled && info.tours.nonEmpty option frag(
                st.section(cls := "team-show__tour team-events team-tournaments")(
                  h2(a(href := routes.Team.tournaments(t.id))(trans.tournaments())),
                  table(cls := "slist")(
                    tournaments.renderList(
                      info.tours.next ::: info.tours.past.take(5 - info.tours.next.size)
                    )
                  )
                )
              ),
              t.enabled && ctx.noKid option
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
    }

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
