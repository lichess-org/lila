package views.team

import play.api.libs.json.Json
import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.TeamInfo
import lila.common.Json.given
import lila.core.socket.SocketVersion
import lila.mod.Modlog
import lila.team.Team

object show:

  import trans.team as trt

  def apply(
      t: Team.WithLeaders,
      members: Paginator[lila.core.LightUser],
      info: TeamInfo,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: Option[SocketVersion],
      asMod: Boolean = false,
      log: List[Modlog] = Nil
  )(using ctx: Context) =
    def havePerm(perm: lila.team.TeamSecurity.Permission.Selector) = info.member.exists(_.hasPerm(perm))
    bits
      .TeamPage(t.name)
      .graph(
        title = s"${t.name} team",
        url = routeUrl(routes.Team.show(t.id)),
        description = t.intro.so { shorten(_, 152) }
      )
      .js(
        PageModule(
          "bits.team",
          Json
            .obj("id" -> t.id)
            .add("socketVersion" -> socketVersion)
            .add("chat" -> chatOption.map: chat =>
              views.chat.json(
                chat.chat,
                chat.lines,
                name = if t.isChatFor(_.Leaders) then trt.leadersChat.txt() else trans.site.chatRoom.txt(),
                timeout = chat.timeout,
                public = true,
                resource = lila.core.chat.PublicSource.Team(t.id),
                localMod = havePerm(_.Comm)
              ))
        )
      )
      .flag(_.noRobots, !t.team.enabled):
        val canManage = asMod && isGranted(_.ManageTeam)
        val canSeeMembers = canManage || (t.enabled && (t.publicMembers || info.mine))
        main(
          cls := "team-show box",
          socketVersion.map: v =>
            data("socket-version") := v
        )(
          boxTop(
            h1(cls := "text", dataIcon := Icon.Group)(t.name, teamFlair(t.team)),
            div:
              if t.disabled then span(cls := "staff")("CLOSED")
              else
                canSeeMembers.option(a(href := routes.Team.members(t.slug)):
                  trt.nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize)))
          ),
          div(cls := "team-show__content")(
            div(cls := "team-show__content__col1")(
              (t.enabled || info.ledByMe || canManage).option(
                st.section(cls := "team-show__meta")(
                  t.publicLeaders.nonEmpty.option(
                    p(
                      trt.teamLeaders.pluralSame(t.publicLeaders.size),
                      ": ",
                      t.publicLeaders.map: l =>
                        userIdLink(l.some)
                    )
                  ),
                  info.ledByMe.option(
                    a(
                      dataIcon := Icon.InfoCircle,
                      href := routes.Cms.lonePage(lila.core.id.CmsPageKey("team-etiquette")),
                      cls := "text"
                    )("Team Etiquette")
                  )
                )
              ),
              (t.enabled && chatOption.isDefined).option(
                frag(
                  views.chat.frag,
                  views.chat.spectatorsFrag
                )
              ),
              bits.actions(t.team, info.member, info.myRequest, info.subscribed, asMod),
              canSeeMembers.option(bits.members(t.team, members))
            ),
            div(cls := "team-show__content__col2")(
              standardFlash,
              (t.intro.isEmpty && havePerm(_.Settings)).option(
                div(cls := "flash flash-warning")(
                  div(cls := "flash__content"):
                    a(href := routes.Team.edit(t.id))("Give your team a short introduction text!")
                )
              ),
              log.nonEmpty.option(renderLog(log)),
              (t.enabled || canManage).option(
                st.section(cls := "team-show__desc")(
                  bits.markdown(t.team, t.descPrivate.ifTrue(info.mine || canManage) | t.description)
                )
              ),
              (t.enabled && info.hasRequests).option(
                div(cls := "team-show__requests")(
                  h2(trt.xJoinRequests.pluralSame(info.requests.size)),
                  request.list(info.requests, t.team.some)
                )
              ),
              div(
                (t.enabled && canSeeMembers && info.simuls.nonEmpty).option(
                  frag(
                    st.section(cls := "team-show__tour team-events team-simuls")(
                      h2(trans.site.simultaneousExhibitions()),
                      views.simul.ui.allCreated(info.simuls)
                    )
                  )
                ),
                (t.enabled && canSeeMembers && info.tours.nonEmpty).option(
                  st.section(cls := "team-show__tour team-events team-tournaments")(
                    h2(a(href := routes.Team.tournaments(t.id))(trans.site.tournaments())),
                    table(cls := "slist")(
                      tournaments.renderList(
                        info.tours.next ::: info.tours.past.take(5 - info.tours.next.size)
                      )
                    )
                  )
                ),
                info.forum.map: forumPosts =>
                  st.section(cls := "team-show__forum")(
                    h2(a(href := teamForumUrl(t.id))(trans.site.forum())),
                    forumPosts.take(10).map { post =>
                      a(cls := "team-show__forum__post", href := routes.ForumPost.redirect(post.post.id))(
                        div(cls := "meta")(
                          strong(post.topic.name),
                          em(
                            post.post.userId.map(titleNameOrId),
                            " • ",
                            momentFromNow(post.post.createdAt)
                          )
                        ),
                        p(shorten(Markdown(post.post.text).unlink, 200))
                      )
                    },
                    a(cls := "more", href := teamForumUrl(t.id))(t.name, " ", trans.site.forum(), " »")
                  )
              )
            )
          )
        )

  private def renderLog(entries: List[Modlog])(using Context) = div(cls := "team-show__log")(
    h2("Mod log"),
    ul(
      entries.map: e =>
        li(
          userIdLink(e.mod.userId.some),
          " ",
          e.showAction,
          ": ",
          Modlog.explain(e)
        )
    )
  )
