package views.html.team

import controllers.routes
import play.api.libs.json.Json

import lila.app.mashup.TeamInfo
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.common.Json.given
import lila.team.Team
import lila.mod.Modlog
import lila.socket.SocketVersion
import lila.socket.SocketVersion.given

object show:

  import trans.team.*

  def apply(
      t: Team.WithLeaders,
      members: Paginator[lila.common.LightUser],
      info: TeamInfo,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: Option[SocketVersion],
      asMod: Boolean = false,
      log: List[Modlog] = Nil
  )(using ctx: PageContext) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} team",
          url = s"$netBaseUrl${routes.Team.show(t.id).url}",
          description = t.intro so { shorten(_, 152) }
        )
        .some,
      moreJs = jsModuleInit(
        "team",
        Json
          .obj("id" -> t.id)
          .add("socketVersion" -> socketVersion)
          .add("chat" -> chatOption.map: chat =>
            views.html.chat.json(
              chat.chat,
              name = if t.isChatFor(_.LEADERS) then leadersChat.txt() else trans.chatRoom.txt(),
              timeout = chat.timeout,
              public = true,
              resourceId = lila.chat.Chat.ResourceId(s"team/${chat.chat.id}"),
              localMod = info.havePerm(_.Comm)
            ))
      )
    ):
      val canSeeMembers = asMod || (t.enabled && (t.publicMembers || info.mine))
      main(
        cls := "team-show box",
        socketVersion.map: v =>
          data("socket-version") := v
      )(
        boxTop(
          h1(cls := "text", dataIcon := licon.Group)(t.name),
          div:
            if t.disabled then span(cls := "staff")("CLOSED")
            else
              canSeeMembers option a(href := routes.Team.members(t.slug)):
                nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize))
        ),
        div(cls := "team-show__content")(
          div(cls := "team-show__content__col1")(
            (t.enabled || info.ledByMe || asMod) option st.section(cls := "team-show__meta")(
              t.publicLeaders.nonEmpty option p(
                teamLeaders.pluralSame(t.publicLeaders.size),
                ": ",
                fragList(t.publicLeaders.toList.map: l =>
                  userIdLink(l.some))
              ),
              info.ledByMe option a(
                dataIcon := licon.InfoCircle,
                href     := routes.ContentPage.loneBookmark("team-etiquette"),
                cls      := "text"
              )("Team Etiquette")
            ),
            (t.enabled && chatOption.isDefined) option frag(
              views.html.chat.frag,
              views.html.chat.spectatorsFrag
            ),
            div(cls := "team-show__actions")(
              (t.enabled && !info.mine) option frag(
                if info.myRequest.exists(_.declined) then
                  frag(
                    strong(requestDeclined()),
                    a(cls := "button disabled button-metal")(joinTeam())
                  )
                else if info.myRequest.isDefined then
                  frag(
                    strong(beingReviewed()),
                    postForm(action := routes.Team.quit(t.id)):
                      submitButton(cls := "button button-red button-empty confirm")(trans.cancel())
                  )
                else ctx.isAuth option joinButton(t.team)
              ),
              t.enabled && info.mine option
                postForm(
                  cls    := "team-show__subscribe form3",
                  action := routes.Team.subscribe(t.id)
                )(
                  div(
                    span(form3.cmnToggle("team-subscribe", "subscribe", checked = info.subscribed)),
                    label(`for` := "team-subscribe")(subToTeamMessages.txt())
                  )
                ),
              (info.mine && !info.havePerm(_.Admin)) option
                postForm(cls := "quit", action := routes.Team.quit(t.id))(
                  submitButton(cls := "button button-empty button-red confirm")(quitTeam.txt())
                ),
              t.enabled && info.havePerm(_.Tour) option frag(
                a(
                  href     := routes.Tournament.teamBattleForm(t.id),
                  cls      := "button button-empty text",
                  dataIcon := licon.Trophy
                ):
                  span(
                    strong(teamBattle()),
                    em(teamBattleOverview())
                  )
                ,
                a(
                  href     := s"${routes.Tournament.form}?team=${t.id}",
                  cls      := "button button-empty text",
                  dataIcon := licon.Trophy
                ):
                  span(
                    strong(teamTournament()),
                    em(teamTournamentOverview())
                  )
                ,
                a(
                  href     := s"${routes.Swiss.form(t.id)}",
                  cls      := "button button-empty text",
                  dataIcon := licon.Trophy
                ):
                  span(
                    strong(trans.swiss.swissTournaments()),
                    em(swissTournamentOverview())
                  )
              ),
              t.enabled && info.havePerm(_.PmAll) option frag(
                a(
                  href     := routes.Team.pmAll(t.id),
                  cls      := "button button-empty text",
                  dataIcon := licon.Envelope
                ):
                  span(
                    strong(messageAllMembers()),
                    em(messageAllMembersOverview())
                  )
              ),
              ((t.enabled && info.havePerm(_.Settings)) || asMod) option
                a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := licon.Gear)(
                  trans.settings.settings()
                ),
              ((t.enabled && info.havePerm(_.Admin)) || asMod) option
                a(
                  cls      := "button button-empty text",
                  href     := routes.Team.leaders(t.id),
                  dataIcon := licon.Group
                )(teamLeaders()),
              ((t.enabled && info.havePerm(_.Kick)) || asMod) option
                a(
                  cls      := "button button-empty text",
                  href     := routes.Team.kick(t.id),
                  dataIcon := licon.InternalArrow
                )(kickSomeone()),
              ((t.enabled && info.havePerm(_.Request)) || asMod) option
                a(
                  cls      := "button button-empty text",
                  href     := routes.Team.declinedRequests(t.id),
                  dataIcon := licon.Cancel
                )(
                  declinedRequests()
                ),
              ((isGranted(_.ManageTeam) || isGranted(_.Shusher)) && !asMod) option a(
                href := routes.Team.show(t.id, 1, mod = true),
                cls  := "button button-red"
              ):
                "View team as Mod"
            ),
            canSeeMembers option div(cls := "team-show__members")(
              st.section(cls := "recent-members")(
                h2(a(href := routes.Team.members(t.slug))(teamRecentMembers())),
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
            standardFlash,
            t.intro.isEmpty && info.havePerm(_.Settings) option div(cls := "flash flash-warning")(
              div(cls := "flash__content"):
                a(href := routes.Team.edit(t.id))("Give your team a short introduction text!")
            ),
            log.nonEmpty option renderLog(log),
            (t.enabled || asMod) option st.section(cls := "team-show__desc")(
              bits.markdown(t.team, t.descPrivate.ifTrue(info.mine) | t.description)
            ),
            t.enabled && info.hasRequests option div(cls := "team-show__requests")(
              h2(xJoinRequests.pluralSame(info.requests.size)),
              views.html.team.request.list(info.requests, t.team.some)
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
              info.forum map { forumPosts =>
                st.section(cls := "team-show__forum")(
                  h2(a(href := teamForumUrl(t.id))(trans.forum())),
                  forumPosts.take(10).map { post =>
                    a(cls := "team-show__forum__post", href := routes.ForumPost.redirect(post.postId))(
                      div(cls := "meta")(
                        strong(post.topicName),
                        em(
                          post.userId map titleNameOrId,
                          " • ",
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

  // handle special teams here
  private def joinButton(t: Team)(using PageContext) =
    t.id.value match
      case "english-chess-players" => joinAt("https://ecf.octoknight.com/")
      case "ecf"                   => joinAt(routes.Team.show("english-chess-players").url)
      case _ =>
        postForm(cls := "inline", action := routes.Team.join(t.id))(
          submitButton(cls := "button button-green")(joinTeam())
        )

  private def joinAt(url: String)(using PageContext) =
    a(cls := "button button-green", href := url)(joinTeam())

  private def renderLog(entries: List[Modlog])(using PageContext) = div(cls := "team-show__log")(
    h2("Mod log"),
    ul(
      entries.map { e =>
        li(
          userIdLink(e.mod.userId.some),
          " ",
          e.showAction,
          ": ",
          Modlog.explain(e)
        )
      }
    )
  )
