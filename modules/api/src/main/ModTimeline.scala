package lila.api

import java.time.LocalDate

import lila.mod.{ Modlog, ModlogApi }
import lila.appeal.{ Appeal, AppealMsg, AppealApi }
import lila.user.{ Note, NoteApi }
import lila.report.{ Report, ReportApi }
import lila.playban.{ TempBan, PlaybanApi }
import lila.shutup.{ PublicLine, ShutupApi }
import lila.core.perm.{ Permission, Granter }
import lila.core.userId.ModId
import lila.report.Reason

case class ModTimeline(
    user: User,
    modLog: List[Modlog],
    appeal: Option[Appeal],
    notes: List[Note],
    reports: List[Report],
    playbanRecord: Option[lila.playban.UserRecord],
    flaggedPublicLines: List[PublicLine]
):
  import ModTimeline.{ *, given }

  lazy val all: List[Event] =
    val reportEvents: List[Event] = reports.flatMap: r =>
      r.done.map(ReportClose(r, _)).toList ::: reportAtoms(r)
    val appealMsgs: List[Event] = appeal.so: a =>
      a.msgs.toList.takeWhile: msg =>
        a.mutedSince.fold(true)(msg.at.isBefore)
    val playBans: List[Event] = playbanRecord.so(_.bans.toList).toNel.map(PlayBans(_)).toList
    val concat: List[Event] =
      modLog ::: appealMsgs ::: notes ::: reportEvents ::: playBans ::: flaggedPublicLines
    // latest first
    concat.sorted(using Ordering.by(at).reverse)

object ModTimeline:

  case class ReportNewAtom(report: Report, atoms: NonEmptyList[Report.Atom])
  case class ReportClose(report: Report, done: Report.Done)
  case class PlayBans(list: NonEmptyList[TempBan])

  type Event = Modlog | AppealMsg | Note | ReportNewAtom | ReportClose | PlayBans | PublicLine

  def aggregateEvents(events: List[Event]): List[Event] =
    events.foldLeft(List.empty[Event])(mergeMany)

  private def mergeMany(prevs: List[Event], next: Event): List[Event] = (prevs, next) match
    case (Nil, n)          => List(n)
    case (head :: rest, n) => mergeOne(head, n).fold(head :: mergeMany(rest, n))(_ :: rest)

  private def mergeOne(prev: Event, next: Event): Option[Event] = (prev, next) match
    case (p: PublicLine, n: PublicLine) => PublicLine.merge(p, n)
    case (p: PlayBans, n: PlayBans)     => PlayBans(n.list ::: p.list).some
    case _                              => none

  private def reportAtoms(report: Report): List[ReportNewAtom | PublicLine] =
    report.atoms
      .groupBy(_.text)
      .values
      .toList
      .flatMap: atoms =>
        atoms.head.parseFlag match
          case None       => List(ReportNewAtom(report, atoms))
          case Some(flag) => flag.quotes.map(PublicLine(_, flag.source, atoms.head.at))

  extension (e: Event)
    def key: String = e match
      case _: Modlog        => "modlog"
      case _: AppealMsg     => "appeal"
      case _: Note          => "note"
      case _: ReportNewAtom => "report-new"
      case _: ReportClose   => "report-close"
      case _: PlayBans      => "playban"
      case _: PublicLine    => "flagged-line"
    def flair: Flair = Flair:
      e match
        case e: Modlog =>
          if e.action == Modlog.permissions then "objects.key"
          else if e.action == Modlog.modMessage then "objects.megaphone"
          else if e.action == Modlog.garbageCollect then "objects.broom"
          else if e.action == Modlog.selfCloseAccount then "objects.locked"
          else if e.action == Modlog.reopenAccount then "objects.unlocked"
          else if Modlog.isSentence(e.action) then "objects.hammer"
          else if Modlog.isUndo(e.action) then "symbols.recycling-symbol"
          else "objects.wrench"
        case _: AppealMsg     => "symbols.left-speech-bubble"
        case _: Note          => "objects.label"
        case _: ReportNewAtom => "symbols.exclamation-mark"
        case _: ReportClose   => "objects.package"
        case _: PlayBans      => "objects.hourglass-not-done"
        case _: PublicLine    => "symbols.exclamation-mark"
    def at: Instant = e match
      case e: Modlog               => e.date
      case e: AppealMsg            => e.at
      case e: Note                 => e.date
      case ReportNewAtom(_, atoms) => atoms.head.at
      case ReportClose(_, done)    => done.at
      case e: PlayBans             => e.list.head.date
      case e: PublicLine           => e.date
    def url(u: User): String = e match
      case _: AppealMsg => routes.Appeal.show(u.username).url
      case _: Note      => s"${routes.User.show(u.username)}?notes=1"
      case _            => s"${routes.User.show(u.username)}?mod=1"

  enum Angle:
    case None
    case Comm
    case Play
  object Angle:
    def filter(e: Event)(using angle: Angle): Boolean = e match
      case _: PlayBans                                                        => angle != Angle.Comm
      case _: ReportClose                                                     => angle != Angle.Comm
      case l: Modlog if l.action == Modlog.chatTimeout && angle != Angle.Comm => false
      case l: Modlog if l.action == Modlog.modMessage =>
        angle match
          case Comm => !l.details.has(lila.playban.PlaybanFeedback.sittingAutoPreset.name)
          case _    => true
      case _ => true

final class ModTimelineApi(
    modLogApi: ModlogApi,
    appealApi: AppealApi,
    noteApi: NoteApi,
    reportApi: ReportApi,
    playBanApi: PlaybanApi,
    shutupApi: ShutupApi,
    userRepo: lila.user.UserRepo
)(using Executor)(using scheduler: Scheduler):

  def load(user: User, withPlayBans: Boolean)(using Me): Fu[ModTimeline] =
    for
      modLogAll <- Granter(_.ModLog).so(modLogApi.userHistory(user.id))
      modLog = modLogAll.filter(filterModLog)
      appeal   <- Granter(_.Appeals).so(appealApi.byId(user))
      notesAll <- noteApi.getForMyPermissions(user, Max(50))
      notes = notesAll.filter(filterNote)
      reportsAll <- Granter(_.SeeReport).so(reportApi.allReportsAbout(user, Max(50)))
      reports = reportsAll.filter(filterReport)
      playban <- withPlayBans.so(Granter(_.SeeReport)).so(playBanApi.fetchRecord(user))
      lines   <- Granter(_.ChatTimeout).so(shutupApi.getPublicLines(user.id))
    yield ModTimeline(user, modLog, appeal, notes, reports, playban, lines)

  private def filterModLog(l: Modlog): Boolean =
    if l.action == Modlog.teamKick && !modsList.contains(l.mod) then false
    else if l.action == Modlog.teamEdit && !modsList.contains(l.mod) then false
    else true

  private def filterNote(note: Note): Boolean =
    if note.from.is(UserId.irwin) then false
    else if note.text.startsWith("Appeal reply:") then false
    else true

  private def filterReport(r: Report): Boolean = !r.isSpontaneous

  private object modsList:
    var all: Set[ModId]               = Set(UserId.lichess.into(ModId))
    def contains(mod: ModId): Boolean = all.contains(mod)
    scheduler.scheduleWithFixedDelay(19 seconds, 1 hour): () =>
      userRepo
        .userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList)
        .foreach: ids =>
          all = ids.map(_.into(ModId)).toSet
