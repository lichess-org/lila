package lila.timeline
package ui

import lila.core.timeline.*
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TimelineUi(helpers: Helpers)(
    streamerLink: UserStr => Tag
):
  import helpers.{ *, given }

  def entries(entries: Vector[Entry])(using Context) =
    div(cls := "entries"):
      filterEntries(entries).map: entry =>
        div(cls := "entry")(renderEntry(entry))

  def more(entries: Vector[Entry])(using Context) =
    Page(trans.site.timeline.txt())
      .css("bits.slist"):
        main(cls := "timeline page-small box")(
          h1(cls := "box__top")(trans.site.timeline()),
          table(cls := "slist slist-pad"):
            tbody:
              filterEntries(entries).map: e =>
                tr(td(renderEntry(e)))
        )

  private def filterEntries(entries: Vector[Entry])(using ctx: Context) =
    if ctx.kid.no then entries
    else entries.filter(_.okForKid)

  private def userLink(userId: UserId)(using ctx: Context) = ctx.me match
    case Some(me) if me.is(userId) => lightUserLink(me.light, withOnline = true)(cls := "online")
    case _                         => userIdLink(userId.some, withOnline = true)

  private def renderEntry(e: Entry)(using ctx: Context) =
    frag(
      e.decode.map[Frag]:
        case Follow(u1, u2) => trans.site.xStartedFollowingY(userLink(u1), userLink(u2))
        case TeamJoin(userId, teamId) =>
          trans.site.xJoinedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case TeamCreate(userId, teamId) =>
          trans.site.xCreatedTeamY(userLink(userId), teamLink(teamId, withIcon = false))
        case ForumPost(userId, _, topicName, postId) =>
          trans.site.xPostedInForumY(
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
          trans.site.xCompetesInY(
            userLink(userId),
            a(href := routes.Tournament.show(tourId))(tourName)
          )
        case SimulCreate(userId, simulId, simulName) =>
          trans.site.xHostsY(
            userLink(userId),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case SimulJoin(userId, simulId, simulName) =>
          trans.site.xJoinsY(
            userLink(userId),
            a(href := routes.Simul.show(simulId))(simulName)
          )
        case GameEnd(playerId, opponent, win, perfKey) =>
          (win match
            case Some(true)  => trans.site.victoryVsYInZ
            case Some(false) => trans.site.defeatVsYInZ
            case None        => trans.site.drawVsYInZ
          )(
            a(
              href     := routes.Round.player(playerId),
              dataIcon := perfKey.perfIcon,
              cls      := "text glpt"
            )(win match
              case Some(true)  => trans.site.victory()
              case Some(false) => trans.site.defeat()
              case None        => trans.site.draw()),
            userIdLink(opponent),
            perfKey.perfTrans
          )
        case StudyLike(userId, studyId, studyName) =>
          trans.site.xLikesY(
            userLink(userId),
            a(href := routes.Study.show(studyId))(studyName)
          )
        case PlanStart(userId) =>
          trans.patron.xBecamePatron(userLink(userId))
        case PlanRenew(userId, months) =>
          trans.patron.xIsPatronForNbMonths
            .plural(months, userLink(userId), months)
        case UblogPostLike(userId, postId, postTitle) =>
          trans.site.xLikesY(
            userLink(userId),
            a(href := routes.Ublog.redirect(postId))(postTitle)
          )
        case StreamStart(id, name) =>
          streamerLink(id.into(UserStr))(cls := "text", dataIcon := Icon.Mic)(
            trans.site.xStartedStreaming(name)
          )
      ,
      " ",
      momentFromNowWithPreload(e.date)
    )
