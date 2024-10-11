package lila.api

import lila.mod.{ Modlog, ModlogApi }
import lila.appeal.{ Appeal, AppealMsg, AppealApi }
import lila.user.{ Note, NoteApi }
import lila.report.{ Report, ReportApi }
import lila.playban.{ TempBan, PlaybanApi }
import lila.shutup.{ PublicLine, ShutupApi }
import java.time.LocalDate
import lila.core.perm.Granter

case class ModTimeline(
    user: User,
    modLog: List[Modlog],
    appeal: Option[Appeal],
    notes: List[Note],
    reports: List[Report],
    playban: Option[lila.playban.UserRecord],
    flaggedPublicLines: List[PublicLine]
):
  import ModTimeline.{ *, given }

  lazy val all: List[Event] =
    val reportEvents: List[Event] = reports.flatMap: r =>
      r.done.map(ReportClose(r, _)).toList ::: r.atoms.toList.map(ReportNewAtom(r, _))
    val appealMsgs: List[Event] = appeal.so(_.msgs.toList)
    val concat: List[Event] =
      modLog ::: appealMsgs ::: notes ::: reportEvents ::: playban.so(_.bans.toList) ::: flaggedPublicLines
    concat.sorted

  private val dayOrdering = summon[Ordering[LocalDate]].reverse
  def allGroupedByDay: List[(LocalDate, List[Event])] =
    all.groupBy(_.at.date).toList.sortBy(_._1)(using dayOrdering)

  // def commReportsAbout: List[Report] = reports
  //   .collect:
  //     case Report.AndAtom(r, _) if r.is(_.Comm) => r
  //   .distinct

object ModTimeline:

  case class ReportNewAtom(report: Report, atom: Report.Atom)
  case class ReportClose(report: Report, done: Report.Done)

  type Event = Modlog | AppealMsg | Note | ReportNewAtom | ReportClose | TempBan | PublicLine

  extension (e: Event)
    def key: String = e match
      case _: Modlog        => "modlog"
      case _: AppealMsg     => "appeal"
      case _: Note          => "note"
      case _: ReportNewAtom => "report-new"
      case _: ReportClose   => "report-close"
      case _: TempBan       => "playban"
      case _: PublicLine    => "flagged-line"
    def flair: Flair = Flair:
      e match
        case e: Modlog =>
          if e.action == Modlog.permissions then "objects.key"
          else if e.action == Modlog.modMessage then "objects.megaphone"
          else if e.action == Modlog.garbageCollect then "objects.broom"
          else if Modlog.isSentence(e.action) then "objects.hammer"
          else if Modlog.isUndo(e.action) then "symbols.recycling-symbol"
          else "objects.wrench"
        case _: AppealMsg     => "symbols.left-speech-bubble"
        case _: Note          => "objects.label"
        case _: ReportNewAtom => "symbols.exclamation-mark"
        case _: ReportClose   => "symbols.large-green-circle"
        case _: TempBan       => "objects.hourglass-not-done"
        case _: PublicLine    => "symbols.triangular-flag"
    def at: Instant = e match
      case e: Modlog            => e.date
      case e: AppealMsg         => e.at
      case e: Note              => e.date
      case ReportNewAtom(_, a)  => a.at
      case ReportClose(_, done) => done.at
      case e: TempBan           => e.date
      case e: PublicLine        => e.date | nowInstant
    def url(u: User): String = e match
      case _: AppealMsg => routes.Appeal.show(u.username).url
      case _: Note      => s"${routes.User.show(u.username)}?notes=1"
      case _            => s"${routes.User.show(u.username)}?mod=1"

  // latest first
  given Ordering[Event] = Ordering.by(at).reverse

final class ModTimelineApi(
    modLogApi: ModlogApi,
    appealApi: AppealApi,
    noteApi: NoteApi,
    reportApi: ReportApi,
    playBanApi: PlaybanApi,
    shutupApi: ShutupApi
)(using Executor):

  def apply(user: User)(using Me): Fu[ModTimeline] =
    for
      modLogAll <- Granter(_.ModLog).so(modLogApi.userHistory(user.id))
      modLog = modLogAll.filter(_.action != Modlog.appealPost)
      appeal  <- Granter(_.Appeals).so(appealApi.byId(user))
      notes   <- noteApi.getForMyPermissions(user)
      reports <- Granter(_.SeeReport).so(reportApi.allReportsAbout(user, Max(50)))
      playban <- Granter(_.SeeReport).so(playBanApi.fetchRecord(user))
      lines   <- Granter(_.ChatTimeout).so(shutupApi.getPublicLines(user.id))
    yield ModTimeline(user, modLog, appeal, notes, reports, playban, lines)
