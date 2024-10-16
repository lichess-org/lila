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
      r.done.map(ReportClose(r, _)).toList :::
        r.atoms.groupBy(_.text).values.toList.map(ReportNewAtom(r, _))
    val appealMsgs: List[Event] = appeal.so(_.msgs.toList)
    val concat: List[Event] =
      modLog ::: appealMsgs ::: notes ::: reportEvents ::: playban.so(_.bans.toList) ::: flaggedPublicLines
    // latest first
    concat.sorted(using Ordering.by(at).reverse)

  // def commReportsAbout: List[Report] = reports
  //   .collect:
  //     case Report.AndAtom(r, _) if r.is(_.Comm) => r
  //   .distinct

object ModTimeline:

  case class ReportNewAtom(report: Report, atoms: NonEmptyList[Report.Atom])
  case class ReportClose(report: Report, done: Report.Done)

  type Event = Modlog | AppealMsg | Note | ReportNewAtom | ReportClose | TempBan | PublicLine

  def aggregateEvents(events: List[Event]): List[Event] =
    events.toNel.fold(events):
      case NonEmptyList(head, tail) =>
        tail
          .foldLeft(NonEmptyList.one(head)):
            case (acc, event) =>
              ModTimeline
                .merge(acc.head, event)
                .fold(event :: acc): merged =>
                  acc.copy(head = merged)
          .toList

  private def merge(prev: Event, next: Event): Option[Event] = (prev, next) match
    case (p: PublicLine, n: PublicLine) => p.copy(text = s"${n.text}|${p.text}").some
    case _                              => none

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
          else if e.action == Modlog.selfCloseAccount then "objects.locked"
          else if e.action == Modlog.reopenAccount then "objects.unlocked"
          else if Modlog.isSentence(e.action) then "objects.hammer"
          else if Modlog.isUndo(e.action) then "symbols.recycling-symbol"
          else "objects.wrench"
        case _: AppealMsg     => "symbols.left-speech-bubble"
        case _: Note          => "objects.label"
        case _: ReportNewAtom => "symbols.exclamation-mark"
        case _: ReportClose   => "objects.package"
        case _: TempBan       => "objects.hourglass-not-done"
        case _: PublicLine    => "symbols.exclamation-mark"
    def at: Instant = e match
      case e: Modlog               => e.date
      case e: AppealMsg            => e.at
      case e: Note                 => e.date
      case ReportNewAtom(_, atoms) => atoms.head.at
      case ReportClose(_, done)    => done.at
      case e: TempBan              => e.date
      case e: PublicLine           => e.date
    def url(u: User): String = e match
      case _: AppealMsg => routes.Appeal.show(u.username).url
      case _: Note      => s"${routes.User.show(u.username)}?notes=1"
      case _            => s"${routes.User.show(u.username)}?mod=1"

  enum Angle:
    case None
    case Comm
    case Play

final class ModTimelineApi(
    modLogApi: ModlogApi,
    appealApi: AppealApi,
    noteApi: NoteApi,
    reportApi: ReportApi,
    playBanApi: PlaybanApi,
    shutupApi: ShutupApi,
    userRepo: lila.user.UserRepo
)(using Executor)(using scheduler: Scheduler):

  def apply(user: User, withPlayBans: Boolean)(using Me): Fu[ModTimeline] =
    for
      modLogAll <- Granter(_.ModLog).so(modLogApi.userHistory(user.id))
      modLog = modLogAll.filter(filterModLog)
      appeal   <- Granter(_.Appeals).so(appealApi.byId(user))
      notesAll <- noteApi.getForMyPermissions(user, Max(50))
      notes = notesAll.filter(filterNote)
      reports <- Granter(_.SeeReport).so(reportApi.allReportsAbout(user, Max(50)))
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

  private object modsList:
    var all: Set[ModId]               = Set(UserId.lichess.into(ModId))
    def contains(mod: ModId): Boolean = all.contains(mod)
    scheduler.scheduleWithFixedDelay(19 seconds, 1 hour): () =>
      userRepo
        .userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList)
        .foreach: ids =>
          all = ids.map(_.into(ModId)).toSet
