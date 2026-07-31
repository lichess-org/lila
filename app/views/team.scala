package views.team

import play.api.libs.json.JsObject
import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.TeamInfo
import lila.core.socket.SocketVersion
import lila.mod.Modlog

private lazy val bits = lila.team.ui.TeamUi(helpers, env.memo.markdown)
export bits.{ list, membersPage }
lazy val form = lila.team.ui.TeamFormUi(helpers, bits)(views.captcha.apply)
lazy val request = lila.team.ui.TeamRequestUi(helpers, bits)
lazy val update = lila.team.ui.TeamUpdateUi(helpers)
lazy val admin = lila.team.ui.TeamAdminUi(helpers, bits)
private lazy val showUi = lila.team.ui.TeamShowUi(helpers, bits, request, update)

def show(
    info: TeamInfo,
    members: Paginator[lila.core.LightUser],
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: Option[SocketVersion],
    asMod: Boolean = false,
    log: List[Modlog] = Nil
)(using ctx: Context) =

  import info.show.team

  val chat: Option[(JsObject, SocketVersion, Frag)] = for
    sv <- socketVersion
    c <- chatOption
    js = views.chat.json(
      c.chat,
      c.lines,
      name = if team.isChatFor(_.Leaders) then trans.team.leadersChat.txt() else trans.site.chatRoom.txt(),
      timeout = c.timeout,
      public = true,
      resource = lila.core.chat.PublicSource.Team(team.id),
      localMod = info.show.havePerm(_.Comm)
    )
    dom = frag(views.chat.frag, views.chat.spectatorsFrag)
  yield (js, sv, dom)

  val modLog = log.map: e =>
    li(userIdLink(e.mod.userId.some), " ", e.showAction, ": ", Modlog.explain(e))

  val toursFrag = info.tours.nonEmpty.option:
    tournaments.renderList:
      info.tours.next ::: info.tours.past.take(5 - info.tours.next.size)

  val forumFrag = info.forum.map:
    _.map: post =>
      a(cls := "team-show__forum__post", href := routes.ForumPost.redirect(post.post.id))(
        div(cls := "meta")(
          strong(post.topic.name),
          em(
            post.post.userId.map(titleNameOrId),
            span(" • "),
            momentFromNow(post.post.createdAt)
          )
        ),
        p(shorten(Markdown(post.post.text).unlink, 210))
      )

  showUi(info.show, members, chat, toursFrag, forumFrag, asMod, modLog)

def updateEventLinks(
    tours: List[lila.tournament.Tournament],
    swiss: List[lila.swiss.Swiss]
)(using Context): List[(Tag, Instant, Call)] =
  tours.map(t => (views.tournament.ui.tournamentLink(t), t.startsAt, routes.Tournament.show(t.id)))
    ++ swiss.map(s => (views.swiss.ui.link(s), s.startsAt, routes.Swiss.show(s.id)))

// both arena and swiss
object tournaments:

  def page(t: lila.team.Team, tours: TeamInfo.PastAndNext)(using Context) =
    Page(s"${t.name} • ${trans.site.tournaments.txt()}")
      .graph(
        title = s"${t.name} team tournaments",
        url = routeUrl(routes.Team.tournaments(t.id)),
        description = shorten(t.description.unlink, 152)
      )
      .css("team")
      .flag(_.fullScreen):
        main(
          div(cls := "box")(
            boxTop:
              h1(teamLink(t, true), " • ", trans.site.tournaments())
            ,
            div(cls := "team-events team-tournaments team-tournaments--both")(
              div(cls := "team-tournaments__next")(
                h2(trans.team.upcomingTournaments()),
                table(cls := "slist slist-pad slist-invert")(
                  renderList(tours.next)
                )
              ),
              div(cls := "team-tournaments__past")(
                h2(trans.team.completedTourns()),
                table(cls := "slist slist-pad")(
                  renderList(tours.past)
                )
              )
            )
          )
        )

  def renderList(tours: List[TeamInfo.AnyTour])(using Context) =
    tbody:
      tours.map:
        _.fold(
          views.tournament.ui.teamTournamentRow,
          views.swiss.ui.teamSwissRow
        )
