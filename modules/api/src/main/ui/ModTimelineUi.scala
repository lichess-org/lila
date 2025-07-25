package lila.api
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.mod.Modlog
import lila.appeal.AppealMsg
import lila.user.Note
import lila.core.config.NetDomain
import lila.core.userId.ModId
import lila.shutup.{ PublicLine, Analyser }
import lila.core.shutup.PublicSource
import lila.core.i18n.Translate

final class ModTimelineUi(helpers: Helpers)(
    publicLineSource: PublicSource => Translate ?=> Frag
)(using NetDomain):
  import helpers.*
  import ModTimeline.*

  def renderGeneral(t: ModTimeline)(using Translate) = render(t)(using Angle.None)
  def renderComm(t: ModTimeline)(using Translate) = render(t)(using Angle.Comm)
  def renderPlay(t: ModTimeline)(using Translate) = render(t)(using Angle.Play)

  private def render(t: ModTimeline)(using angle: Angle)(using Translate) = div(cls := "mod-timeline"):
    val today = nowInstant.date
    t.all
      .filter(Angle.filter)
      .map: e =>
        if e.at.date == today
        then momentFromNowServerText(e.at) -> e
        else daysFromNow(e.at.date) -> e
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toList
      .sortByReverse(_._2.head.at)
      .map: (period, events) =>
        (period, ModTimeline.aggregateEvents(events))
      .map(renderPeriod(t))

  private def renderPeriod(t: ModTimeline)(period: (String, List[Event]))(using Translate) =
    div(cls := "mod-timeline__period")(
      h3(period._1),
      div(cls := "mod-timeline__period__events")(period._2.map(renderEvent(t)))
    )

  private def renderEvent(t: ModTimeline)(e: Event)(using Translate) =
    val isRecent = e.at.isAfter(nowInstant.minusMonths(6))
    div(
      cls := List(
        "mod-timeline__event" -> true,
        s"mod-timeline__event--${e.key}" -> true,
        "mod-timeline__event--recent" -> isRecent
      )
    )(
      a(cls := "mod-timeline__event__flair", href := e.url(t.user)):
        img(src := flairSrc(e.flair), title := e.key, datetimeAttr := isoDateTime(e.at))
      ,
      div(cls := "mod-timeline__event__body")(renderEventBody(t)(e))
    )

  private def renderEventBody(t: ModTimeline)(e: Event)(using Translate): Frag =
    e match
      case e: Modlog => renderModlog(t.user)(e)
      case e: AppealMsg => renderAppeal(t)(e)
      case e: Note => renderNote(e)
      case e: ReportNewAtom => renderReportNew(e)
      case e: PlayBans => renderPlayBans(e)
      case e: ReportLineFlag => renderReportLineFlag(e)
      case AccountCreation(_) => renderAccountCreation(t.user)

  private def renderMod(userId: ModId)(using Translate) =
    if userId.is(UserId.lichess) then span(iconFlair(Flair("smileys.robot")), "Lichess")
    else userIdLink(userId.some, withTitle = false, modIcon = true)
  private def renderUser(userId: UserId)(using Translate) =
    userIdLink(userId.some, withTitle = false)

  private def renderText(str: String, highlightBad: Boolean) =
    div(cls := "mod-timeline__text"):
      val short = shorten(str, 200)
      if highlightBad then Analyser.highlightBad(short)
      else short

  private def renderPlayBans(e: PlayBans) =
    frag(pluralize("Playban", e.list.size), ": ", e.list.map(_.mins).toList.mkString(", "), " minutes")

  private def renderReportLineFlag(r: ReportLineFlag)(using Translate) = frag(
    r.openId match
      case Some(reportId) =>
        postForm(action := s"${routes.Report.inquiry(reportId.value)}?onlyOpen=1")(
          cls := "mod-timeline__report-form mod-timeline__report-form--open"
        )(submitButton("Chat flag")(cls := "button button-thin", title := "Open"))
      case _ => "Chat flag",
    publicLineSource(r.line.from),
    div(cls := "mod-timeline__texts")(
      fragList(
        PublicLine.merge
          .split(r.line.text)
          .map(quote => span(cls := "message")(Analyser.highlightBad(quote))),
        " | "
      )
    )
  )

  private def renderReportNew(r: ReportNewAtom)(using Translate) =
    import r.*
    frag(
      if r.atoms.size == 1 && r.atoms.forall(_.by.is(UserId.lichess))
      then renderMod(UserId.lichess.into(ModId))
      else
        strong(
          cls := "mod-timeline__event__from",
          title := r.atoms.toList.map(_.by.id).map(usernameOrId).map(_.toString).mkString(", ")
        )(
          if r.atoms.size > 3
          then pluralize("player", r.atoms.size)
          else
            fragList:
              r.atoms.toList.map: atom =>
                userIdLink(atom.by.some, withOnline = false)
        )
      ,
      div(cls := "mod-timeline__event__action")(
        " opened a ",
        postForm(action := s"${routes.Report.inquiry(report.id.value)}?onlyOpen=1")(
          cls := List("mod-timeline__report-form" -> true, "mod-timeline__report-form--open" -> r.report.open)
        )(
          submitButton(strong(atoms.head.reason.name), " report")(
            cls := "button button-thin",
            title := r.report.done.fold("Open")(d => s"Closed by ${usernameOrId(d.by.id)}")
          )
        )
      ),
      renderText(atoms.head.text, highlightBad = false)
    )

  private def renderModlog(user: User)(e: Modlog)(using Translate) =
    val author: Frag =
      if e.action == Modlog.selfCloseAccount then renderUser(user.id)
      else renderMod(e.mod)
    frag(
      author,
      span(
        cls := List(
          "mod-timeline__event__action" -> true,
          s"mod-timeline__event__action--${e.action}" -> true,
          "mod-timeline__event__action--warning" -> Modlog.isWarning(e),
          "mod-timeline__event__action--sentence" -> Modlog.isSentence(e.action),
          "mod-timeline__event__action--account-sentence" -> Modlog.isAccountSentence(e.action),
          "mod-timeline__event__action--undo" -> Modlog.isUndo(e.action)
        )
      ):
        if Modlog.isWarning(e) then strong("sends warning")
        else e.showAction
      ,
      div(cls := "mod-timeline__text"):
        e.gameId.fold[Frag](e.details.orZero: String): gameId =>
          a(href := s"${routes.Round.watcher(gameId, Color.white).url}?pov=${e.user.so(_.value)}"):
            e.details.orZero: String
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
      div(cls := "mod-timeline__text"):
        richText(n.text, expandImg = false)
    )

  private def renderAccountCreation(user: User)(using Translate) =
    frag(
      renderUser(user.id),
      "signs up"
    )
