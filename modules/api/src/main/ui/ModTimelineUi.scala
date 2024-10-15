package lila.api
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.mod.Modlog
import lila.appeal.AppealMsg
import lila.user.Note
import lila.report.Report
import lila.playban.TempBan
import java.time.LocalDate
import lila.core.config.NetDomain
import lila.core.userId.ModId
import lila.shutup.{ PublicLine, Analyser }
import lila.core.shutup.PublicSource
import lila.core.i18n.Translate
import lila.core.id.RelayRoundId

final class ModTimelineUi(helpers: Helpers)(
    publicLineSource: PublicSource => Translate ?=> Frag
)(using NetDomain):
  import helpers.{ *, given }
  import ModTimeline.*

  private val eventOrdering = summon[Ordering[Instant]].reverse

  def render(t: ModTimeline)(using Translate) = div(cls := "mod-timeline"):
    t.all
      .map: e =>
        daysFromNow(e.at.date) -> e
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toList
      .sortBy(x => x._2.head.at)(eventOrdering)
      .map(renderPeriod(t))

  private def renderPeriod(t: ModTimeline)(period: (String, List[Event]))(using Translate) =
    div(cls := "mod-timeline__day")(
      h3(period._1),
      div(cls := "mod-timeline__day__events")(period._2.map(renderEvent(t)))
    )

  private def renderEvent(t: ModTimeline)(e: Event)(using Translate) =
    div(cls := s"mod-timeline__event mod-timeline__event--${e.key}")(
      a(cls := "mod-timeline__event__flair", href := e.url(t.user)):
        img(src := flairSrc(e.flair), title := e.key)
      ,
      showTime(e.at),
      div(cls := "mod-timeline__event__body")(renderEventBody(t)(e))
    )

  private def renderEventBody(t: ModTimeline)(e: Event)(using Translate): Frag =
    e match
      case e: Modlog        => renderModlog(t.user)(e)
      case e: AppealMsg     => renderAppeal(t)(e)
      case e: Note          => renderNote(e)
      case e: ReportNewAtom => renderReportNew(e)
      case e: ReportClose   => renderReportClose(e)
      case e: TempBan       => frag("Playban: ", e.mins, " minutes")
      case e: PublicLine    => renderPublicLine(e)

  private def renderMod(userId: ModId)(using Translate) =
    userIdLink(userId.some, withTitle = false, modIcon = true)
  private def renderUser(userId: UserId)(using Translate) =
    userIdLink(userId.some, withTitle = false)

  private def renderText(str: String, highlightBad: Boolean) =
    div(cls := "mod-timeline__text"):
      val short = shorten(str, 200)
      if highlightBad then Analyser.highlightBad(short)
      else short

  private def renderPublicLine(l: PublicLine)(using Translate) = frag(
    renderMod(UserId.lichess.into(ModId)),
    publicLineSource(l.from),
    renderText(l.text, true)
  )

  private def renderReportNew(r: ReportNewAtom)(using Translate) =
    import r.*
    val flag = atoms.head.parseFlag
    frag(
      if r.atoms.size == 1 && r.atoms.head.by.is(UserId.lichess)
      then renderMod(UserId.lichess.into(ModId))
      else strong(cls := "mod-timeline__event__from")(pluralize("player", r.atoms.size)),
      span(cls := "mod-timeline__event__action")(
        flag match
          case Some(f) => publicLineSource(f.source)
          case None =>
            frag(
              " opened a ",
              report.room.name,
              " report about ",
              atoms.head.reason.name
            )
      ),
      flag.fold(renderText(atoms.head.text, false)): flag =>
        renderText(flag.quotes.mkString(" | "), true)
    )

  private def renderReportClose(r: ReportClose)(using Translate) = frag(
    renderMod(r.done.by),
    " closed the ",
    r.report.room.name,
    " report about ",
    r.report.atoms.toList.map(_.reason.name).mkString(", ")
  )

  private def renderModlog(user: User)(e: Modlog)(using Translate) =
    val actionTag = if Modlog.isSentence(e.action) then badTag else span
    val author: Frag =
      if e.action == Modlog.selfCloseAccount then renderUser(user.id)
      else renderMod(e.mod)
    frag(
      author,
      actionTag(cls := "mod-timeline__event__action")(e.showAction),
      div(cls := "mod-timeline__text"):
        e.gameId.fold[Frag](e.details.orZero: String) { gameId =>
          a(href := s"${routes.Round.watcher(gameId, Color.white).url}?pov=${e.user.so(_.value)}")(
            e.details.orZero: String
          )
        }
    )

  private def renderAppeal(t: ModTimeline)(a: AppealMsg)(using Translate) =
    frag(
      if a.by.is(t.user)
      then renderUser(a.by)
      else renderMod(a.by.into(ModId)),
      renderText(a.text, false)
    )

  private def renderNote(n: Note)(using Translate) =
    frag(
      renderMod(n.from.into(ModId)),
      renderText(n.text, false)
    )
