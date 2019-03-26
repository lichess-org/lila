package views.html

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.escapeHtml
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
    base.layout(
      title = trans.timeline.txt(),
      responsive = true,
      moreCss = responsiveCssTag("slist")
    )(
        main(cls := "timeline page-small box")(
          h1(trans.timeline.frag()),
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

  private def entry(e: lila.timeline.Entry)(implicit ctx: Context) = frag(
    e.decode.map[Frag] {
      case Follow(u1, u2) => playHtmlToFrag(trans.xStartedFollowingY(userIdLink(u1.some, withOnline = false), userIdLink(u2.some, withOnline = false)))
      case TeamJoin(userId, teamId) => trans.xJoinedTeamY(userIdLink(userId.some, withOnline = false), teamLink(teamId, withIcon = false))
      case TeamCreate(userId, teamId) => trans.xCreatedTeamY(userIdLink(userId.some, withOnline = false), teamLink(teamId, withIcon = false))
      case ForumPost(userId, topicId, topicName, postId) => trans.xPostedInForumY(userIdLink(userId.some, withOnline = false), Html("""<a href="%s" title="%s">%s</a>""".format(routes.ForumPost.redirect(postId), escapeHtml(topicName), shorten(topicName, 30))))
      case NoteCreate(fromId, toId) => trans.xLeftANoteOnY(userIdLink(fromId.some, withOnline = false), userIdLink(toId.some, withOnline = false, params = "?note"))
      case TourJoin(userId, tourId, tourName) => trans.xCompetesInY(userIdLink(userId.some, withOnline = false), Html("""<a href="%s">%s</a>""".format(routes.Tournament.show(tourId), escapeHtml(tourName))))
      case SimulCreate(userId, simulId, simulName) => trans.xHostsY(userIdLink(userId.some, withOnline = false), Html(s"""<a href="${routes.Simul.show(simulId)}">${escapeHtml(simulName)}</a>"""))
      case SimulJoin(userId, simulId, simulName) => trans.xJoinsY(userIdLink(userId.some, withOnline = false), Html(s"""<a href="${routes.Simul.show(simulId)}">${escapeHtml(simulName)}</a>"""))
      case GameEnd(playerId, opponent, win, perfKey) => lila.rating.PerfType(perfKey) map { perf =>
        trans.xVsYinZ(Html("""<a href="%s" data-icon="%s" class="glpt"> %s</a>""".format(routes.Round.player(playerId), perf.iconChar, win match {
          case Some(true) => trans.victory()
          case Some(false) => trans.defeat()
          case _ => trans.draw()
        })), userIdLink(opponent, withOnline = false), perf.name)
      }
      case StudyCreate(userId, studyId, studyName) => trans.xHostsY(userIdLink(userId.some, withOnline = false), Html(s"""<a href="${routes.Study.show(studyId)}">${escapeHtml(studyName)}</a>"""))
      case StudyLike(userId, studyId, studyName) => trans.xLikesY(userIdLink(userId.some, withOnline = false), Html(s"""<a href="${routes.Study.show(studyId)}">${escapeHtml(studyName)}</a>"""))
      case PlanStart(userId) => frag(
        userIdLink(userId.some, withOnline = true),
        " became a ",
        a(href := routes.Plan.index)("Patron")
      )
      case BlogPost(id, slug, title) => a(cls := "text", dataIcon := "6", href := routes.Blog.show(id, slug))(title)
      case StreamStart(id, name) => a(cls := "text", dataIcon := "", href := routes.Streamer.show(id))(name, " started streaming")
    },
    " ",
    momentFromNow(e.date)
  )
}
