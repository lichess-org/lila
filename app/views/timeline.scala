package views.html

import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.actorApi.timeline._

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

  private def userLink(userId: lila.user.User.ID)(implicit ctx: Context) =
    ctx.me match {
      case Some(me) if me.is(userId) => lightUserLink(me.light, withOnline = true)(ctx.lang)(cls := "online")
      case _                         => userIdLink(userId.some, withOnline = true)
    }

  private def entry(e: lila.timeline.Entry)(implicit ctx: Context) =
    frag(
      e.decode.map[Frag] {
        case Follow(u1, u2) =>
          trans.xStartedFollowingY(
            userLink(u1),
            userLink(u2)
          )
        case TeamJoin(userId, teamId) =>
          trans.xJoinedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case TeamCreate(userId, teamId) =>
          trans.xCreatedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case ForumPost(userId, _, topicName, postId) =>
          trans.xPostedInForumY(
            userLink(userId),
            a(
              href := routes.ForumPost.redirect(postId),
              title := topicName
            )(shorten(topicName, 30))
          )
        case UblogPost(userId, id, slug, title) =>
          trans.ublog.xPublishedY(
            userLink(userId),
            a(
              href := routes.Ublog.post(usernameOrId(userId), slug, id)
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
          for {
            opponentId <- opponent
            perf       <- lila.rating.PerfType(perfKey)
          } yield (win match {
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
            userLink(opponentId),
            perf.trans
          )
        case StudyLike(userId, studyId, studyName) =>
          trans.xLikesY(
            userLink(userId),
            a(href := routes.Study.show(studyId))(studyName)
          )
        case PlanStart(userId) =>
          a(href := routes.Plan.index)(
            trans.patron.xBecamePatron(userLink(userId))
          )
        case PlanRenew(userId, months) =>
          a(href := routes.Plan.index)(
            trans.patron.xIsPatronForNbMonths
              .plural(months, userLink(userId), months)
          )
        case BlogPost(id, slug, title) =>
          a(cls := "text", dataIcon := "", href := routes.Blog.show(id, slug))(title)
        case StreamStart(id, name) =>
          views.html.streamer.bits
            .redirectLink(id)(cls := "text", dataIcon := "")(trans.xStartedStreaming(name))
      },
      " ",
      momentFromNowWithPreload(e.date)
    )
}
