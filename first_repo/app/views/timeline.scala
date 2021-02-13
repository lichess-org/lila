package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.actorApi.timeline._

import controllers.routes

object timeline {

  def entries(entries: Vector[lila.timeline.Entry])(implicit ctx: Context) =
    div(cls := "entries")(
      filterEntries(entries) map { entry =>
        div(cls := "entry")(timeline.entry(entry))
      }
    )

  def more(entries: Vector[lila.timeline.Entry])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.timeline.txt(),
      moreCss = cssTag("slist")
    )(
      main(cls := "timeline page-small box")(
        h1(trans.timeline()),
        table(cls := "slist slist-pad")(
          tbody(
            filterEntries(entries) map { e =>
              tr(td(entry(e)))
            }
          )
        )
      )
    )

  private def filterEntries(entries: Vector[lila.timeline.Entry])(implicit ctx: Context) =
    if (ctx.noKid) entries
    else entries.filter(e => e.okForKid)

  private def entry(e: lila.timeline.Entry)(implicit ctx: Context) =
    frag(
      e.decode.map[Frag] {
        case Follow(u1, u2) =>
          trans.xStartedFollowingY(
            userIdLink(u1.some, withOnline = false),
            userIdLink(u2.some, withOnline = false)
          )
        case TeamJoin(userId, teamId) =>
          trans.xJoinedTeamY(userIdLink(userId.some, withOnline = false), teamLink(teamId, withIcon = false))
        case TeamCreate(userId, teamId) =>
          trans.xCreatedTeamY(userIdLink(userId.some, withOnline = false), teamLink(teamId, withIcon = false))
        case ForumPost(userId, _, topicName, postId) =>
          trans.xPostedInForumY(
            userIdLink(userId.some, withOnline = false),
            a(
              href := routes.ForumPost.redirect(postId),
              title := topicName
            )(shorten(topicName, 30))
          )
        case TourJoin(userId, tourId, tourName) =>
          trans.xCompetesInY(
            userIdLink(userId.some, withOnline = false),
            a(href := routes.Tournament.show(tourId))(tourName)
          )
        case SimulCreate(userId, simulId, simulName) =>
          trans.xHostsY(
            userIdLink(userId.some, withOnline = false),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case SimulJoin(userId, simulId, simulName) =>
          trans.xJoinsY(
            userIdLink(userId.some, withOnline = false),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case GameEnd(playerId, opponent, win, perfKey) =>
          lila.rating.PerfType(perfKey) map { perf =>
            (win match {
              case Some(true)  => trans.victoryVsYInZ
              case Some(false) => trans.defeatVsYInZ
              case None        => trans.drawVsYInZ
            })(
              a(
                href := routes.Round.player(playerId),
                dataIcon := perf.iconChar,
                cls := "text glpt"
              )(win match {
                case Some(true)  => trans.victory()
                case Some(false) => trans.defeat()
                case None        => trans.draw()
              }),
              userIdLink(opponent, withOnline = false),
              perf.trans
            )
          }
        case StudyCreate(userId, studyId, studyName) =>
          trans.xCreatesStudyY(
            userIdLink(userId.some, withOnline = false),
            a(href := routes.Study.show(studyId))(studyName)
          )
        case StudyLike(userId, studyId, studyName) =>
          trans.xLikesY(
            userIdLink(userId.some, withOnline = false),
            a(href := routes.Study.show(studyId))(studyName)
          )
        case PlanStart(userId) =>
          a(href := routes.Plan.index)(
            trans.patron.xBecamePatron(userIdLink(userId.some, withOnline = true))
          )
        case BlogPost(id, slug, title) =>
          a(cls := "text", dataIcon := "6", href := routes.Blog.show(id, slug))(title)
        case StreamStart(id, name) =>
          a(cls := "text", dataIcon := "", href := routes.Streamer.show(id))(trans.xStartedStreaming(name))
      },
      " ",
      momentFromNowWithPreload(e.date)
    )
}
