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

  def render(t: ModTimeline)(using Translate) = div(cls := "mod-timeline"):
    t.allGroupedByDay.map(renderDay(t))

  private def renderDay(t: ModTimeline)(day: (LocalDate, List[Event]))(using Translate) =
    div(cls := "mod-timeline__day")(
      h3(showDate(day._1)),
      div(cls := "mod-timeline__day__events")(day._2.map(renderEvent(t)))
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

  private def renderText(str: String) = div(cls := "mod-timeline__text")(shorten(str, 200))

  private def renderPublicLine(l: PublicLine)(using Translate) = frag(
    renderMod(UserId.lichess.into(ModId)),
    publicLineSource(l.from),
    div(cls := "mod-timeline__txt")(Analyser.highlightBad(l.text))
  )

  private def renderReportNew(r: ReportNewAtom)(using Translate) =
    import r.*
    val flag = atoms.head.parseFlag
    frag(
      strong(cls := "mod-timeline__event__from")(pluralize("player", r.atoms.size)),
      span(cls := "mod-timeline__event__action")(
        flag match
          case Some(f) => publicSourceLink(f.resource)
          case None =>
            frag(
              " opened a ",
              report.room.name,
              " report about ",
              atoms.head.reason.name
            )
      ),
      renderText(flag.fold(atoms.head.text)(_.quote))
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
      renderText(a.text)
    )

  private def renderNote(n: Note)(using Translate) =
    frag(
      renderMod(n.from.into(ModId)),
      renderText(n.text)
    )

  def publicSourceLink(source: String): Option[Frag] =
    source.split("/") match
      case Array(tpe, id) => ModTimelineUi.publicSourceLink(tpe, id).some
      case _              => none

object ModTimelineUi:
  def publicSourceLink(tpe: String, id: String): Frag =
    val path = tpe match
      case "game"       => routes.Round.watcher(GameId(id), Color.white).url
      case "relay"      => routes.RelayRound.show("-", "-", RelayRoundId(id)).url
      case "tournament" => routes.Tournament.show(TourId(id)).url
      case "swiss"      => routes.Swiss.show(SwissId(id)).url
      case "forum"      => routes.ForumPost.redirect(ForumPostId(id)).url
      case _            => s"/$tpe/$id"
    a(href := path)(path)
