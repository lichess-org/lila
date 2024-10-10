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

final class ModTimelineUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }
  import ModTimeline.*

  def render(t: ModTimeline)(using Translate) = div(cls := "mod-timeline"):
    t.allGroupedByDay.map(renderDay(t))

  private def renderDay(t: ModTimeline)(day: (LocalDate, List[Event]))(using Translate) =
    div(cls := "mod-timeline__day")(
      h3(showDate(day._1)),
      div(cls := "mod-timeline__day__events")(day._2.map(renderEvent(t)))
    )

  private def renderEvent(t: ModTimeline)(e: Event)(using Translate) =
    div(cls := s"mod-timeline__event mod-timeline__event--${e.key}")(
      img(cls := "mod-timeline__event__flair", src := flairSrc(e.flair), title := e.key),
      showTime(e.at),
      div(cls := "mod-timeline__event__body")(renderEventBody(t)(e))
    )

  private def renderEventBody(t: ModTimeline)(e: Event)(using Translate): Frag =
    e match
      case e: Modlog        => renderModlog(e)
      case e: AppealMsg     => renderAppeal(t)(e)
      case e: Note          => renderNote(e)
      case e: ReportNewAtom => renderReportNew(e)
      case e: ReportClose   => renderReportClose(e)
      case e: TempBan       => frag("Playban: ", e.mins, " minutes")

  private def renderMod(userId: ModId)(using Translate) =
    userIdLink(userId.some, withTitle = false, modIcon = true)
  private def renderUser(userId: UserId)(using Translate) =
    userIdLink(userId.some, withTitle = false)

  private def renderReportNew(r: ReportNewAtom)(using Translate) =
    import r.*
    frag(
      userIdLink(report.user.some),
      span(cls := "mod-timeline__event__action")(
        " opened a ",
        report.room.name,
        " report about ",
        atom.reason.name
      ),
      div(cls := "mod-timeline__text")(shorten(atom.text, 200))
    )

  private def renderReportClose(r: ReportClose)(using Translate) = frag(
    renderMod(r.done.by),
    " closed the ",
    r.report.room.name,
    " report about ",
    r.report.atoms.toList.map(_.reason.name).mkString(", ")
  )

  private def renderModlog(e: Modlog)(using Translate) =
    val actionTag = if Modlog.isSentence(e.action) then badTag else span
    frag(
      renderMod(e.mod),
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
      div(cls := "mod-timeline__text"):
        richText(a.text, expandImg = false)
    )

  private def renderNote(n: Note)(using Translate) =
    frag(
      renderMod(n.from.into(ModId)),
      div(cls := "mod-timeline__text"):
        richText(n.text, expandImg = false)
    )
