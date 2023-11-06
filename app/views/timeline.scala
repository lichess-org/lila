package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.hub.actorApi.timeline.*

object timeline:

  def entries(entries: Vector[lila.timeline.Entry])(using Context) =
    div(cls := "entries")(
      filterEntries(entries) map { entry =>
        div(cls := "entry")(timeline.entry(entry))
      }
    )

  def more(entries: Vector[lila.timeline.Entry])(using PageContext) =
    views.html.base.layout(
      title = trans.timeline.txt(),
      moreCss = cssTag("slist")
    )(
      main(cls := "timeline page-small box")(
        h1(cls := "box__top")(trans.timeline()),
        table(cls := "slist slist-pad")(
          tbody:
            filterEntries(entries).map: e =>
              tr(td(entry(e)))
        )
      )
    )

  private def filterEntries(entries: Vector[lila.timeline.Entry])(using ctx: Context) =
    if ctx.kid.no then entries
    else entries.filter(_.okForKid)

  private def userLink(userId: UserId)(using ctx: Context) =
    ctx.me match
      case Some(me) if me.is(userId) => lightUserLink(me.light, withOnline = true)(cls := "online")
      case _                         => userIdLink(userId.some, withOnline = true)

  private def entry(e: lila.timeline.Entry)(using ctx: Context) =
    frag(
      e.decode.map[Frag] {
        case Follow(u1, u2) => trans.xStartedFollowingY(userLink(u1), userLink(u2))
        case TeamJoin(userId, teamId) =>
          trans.xJoinedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case TeamCreate(userId, teamId) =>
          trans.xCreatedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case ForumPost(userId, _, topicName, postId) =>
          trans.xPostedInForumY(
            userLink(userId),
            a(
              href  := routes.ForumPost.redirect(postId),
              title := topicName
            )(shorten(topicName, 30))
          )
        case UblogPost(userId, id, slug, title) =>
          trans.ublog.xPublishedY(
            userLink(userId),
            a(
              href     := routes.Ublog.post(usernameOrId(userId), slug, id),
              st.title := title
            )(shorten(title, 40))
          )
        case TourJoin(userId, tourId, tourName) =>
          trans.xCompetesInY(
            userLink(userId),
            a(href := routes.Tournament.show(tourId))(tourName)
          )
        case SimulCreate(userId, simulId, simulName) =>
          trans.xHostsY(
            userLink(userId),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case SimulJoin(userId, simulId, simulName) =>
          trans.xJoinsY(
            userLink(userId),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case GameEnd(playerId, opponent, win, perfKey) =>
          lila.rating.PerfType(lila.rating.Perf.Key(perfKey)) map { perf =>
            (win match
              case Some(true)  => trans.victoryVsYInZ
              case Some(false) => trans.defeatVsYInZ
              case None        => trans.drawVsYInZ
            )(
              a(
                href     := routes.Round.player(playerId),
                dataIcon := perf.icon,
                cls      := "text glpt"
              )(win match
                case Some(true)  => trans.victory()
                case Some(false) => trans.defeat()
                case None        => trans.draw()
              ),
              userIdLink(opponent),
              perf.trans
            )
          }
        case StudyLike(userId, studyId, studyName) =>
          trans.xLikesY(
            userLink(userId),
            a(href := routes.Study.show(studyId))(studyName)
          )
        case PlanStart(userId) =>
          trans.patron.xBecamePatron(userLink(userId))
        case PlanRenew(userId, months) =>
          trans.patron.xIsPatronForNbMonths
            .plural(months, userLink(userId), months)
        case BlogPost(id, slug, title) =>
          a(cls := "text", dataIcon := licon.InkQuill, href := routes.Blog.show(id, slug))(title)
        case UblogPostLike(userId, postId, postTitle) =>
          trans.xLikesY(
            userLink(userId),
            a(href := routes.Ublog.redirect(postId))(postTitle)
          )
        case StreamStart(id, name) =>
          views.html.streamer.bits
            .redirectLink(id)(cls := "text", dataIcon := licon.Mic)(trans.xStartedStreaming(name))
      },
      " ",
      momentFromNowWithPreload(e.date)
    )
