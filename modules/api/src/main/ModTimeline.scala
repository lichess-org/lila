package lila.api

import lila.mod.{ Modlog, ModlogApi }
import lila.appeal.{ Appeal, AppealMsg, AppealApi }
import lila.user.{ Note, NoteApi }
import lila.report.{ Report, ReportApi }
import lila.playban.{ TempBan, PlaybanApi }
import lila.core.perm.{ Permission, Granter }
import lila.core.userId.ModId
import lila.core.id.ReportId
import lila.core.shutup.PublicLine

case class ModTimeline(
    user: User,
    modLog: List[Modlog],
    appeal: Option[Appeal],
    notes: List[Note],
    reports: List[Report],
    playbanRecord: Option[lila.playban.UserRecord]
):
  import ModTimeline.*

  lazy val all: List[Event] =
    val reportEvents: List[Event] = reports.flatMap(reportAtoms)
    val appealMsgs: List[Event]   = appeal.so: a =>
      a.msgs.toList.takeWhile: msg =>
        a.mutedSince.fold(true)(msg.at.isBefore)
    val playBans: List[Event] = playbanRecord.so(_.bans.toList).map(pb => PlayBans(NonEmptyList.one(pb)))
    val accountCreation: List[Event] = List(AccountCreation(user.createdAt))
    val concat: List[Event]          =
      modLog ::: appealMsgs ::: notes ::: reportEvents ::: playBans ::: accountCreation
    // latest first
    concat.sortedReverse(using Ordering.by(at))

object ModTimeline:

  case class ReportNewAtom(report: Report, atoms: NonEmptyList[Report.Atom]):
    def like(r: Report): Boolean = report.room == r.room
  case class ReportLineFlag(openId: Option[ReportId], line: PublicLine):
    def merge(o: ReportLineFlag) = (openId == o.openId).so:
      lila.shutup.PublicLine.merge(line, o.line).map(l => copy(line = l))
  case class PlayBans(list: NonEmptyList[TempBan])
  case class AccountCreation(at: Instant)

  type Event = Modlog | AppealMsg | Note | ReportNewAtom | ReportLineFlag | PlayBans | AccountCreation

  def aggregateEvents(events: List[Event]): List[Event] =
    events.foldLeft(List.empty[Event])(mergeMany)

  private def mergeMany(prevs: List[Event], next: Event): List[Event] = (prevs, next) match
    case (Nil, n)          => List(n)
    case (head :: rest, n) => mergeOne(head, n).fold(head :: mergeMany(rest, n))(_ :: rest)

  private def mergeOne(prev: Event, next: Event): Option[Event] = (prev, next) match
    case (p: ReportLineFlag, n: ReportLineFlag)        => p.merge(n)
    case (p: PlayBans, n: PlayBans)                    => PlayBans(n.list ::: p.list).some
    case (p: AppealMsg, n: AppealMsg) if p.by.is(n.by) => p.copy(text = s"${n.text}\n\n${p.text}").some
    case (p: ReportNewAtom, n: ReportNewAtom) if n.like(p.report) => p.copy(atoms = n.atoms ::: p.atoms).some
    case (p: Modlog, n: Modlog)                                   => mergeModlog(p, n)
    case _                                                        => none

  private def mergeModlog(p: Modlog, n: Modlog): Option[Modlog] =
    (p.action == n.action && p.mod.is(n.mod)).option:
      p.copy(details = some(List(p.details, n.details).flatten.distinct.mkString(" / ")))

  private def reportAtoms(report: Report): List[ReportNewAtom | ReportLineFlag] =
    report.atoms
      .groupBy(_.text)
      .values
      .toList
      .flatMap: atoms =>
        atoms.head.parseFlag match
          case None       => List(ReportNewAtom(report, atoms))
          case Some(flag) =>
            flag.quotes.map(text =>
              ReportLineFlag(report.open.option(report.id), PublicLine(text, flag.source, atoms.head.at))
            )

  extension (e: Event)
    def key: String = e match
      case _: Modlog          => "modlog"
      case _: AppealMsg       => "appeal"
      case _: Note            => "note"
      case _: ReportNewAtom   => "report-new"
      case _: PlayBans        => "playban"
      case _: ReportLineFlag  => "flagged-line"
      case _: AccountCreation => "account-creation"
    def flair: Flair = Flair:
      e match
        case e: Modlog =>
          if e.action == Modlog.permissions then "objects.key"
          else if Modlog.isWarning(e) then "symbols.large-yellow-square"
          else if e.action == Modlog.modMessage then "objects.megaphone"
          else if e.action == Modlog.garbageCollect then "objects.broom"
          else if e.action == Modlog.selfCloseAccount then "objects.locked"
          else if e.action == Modlog.reopenAccount then "objects.unlocked"
          else if Modlog.isSentence(e.action) then "objects.hammer"
          else if Modlog.isUndo(e.action) then "symbols.recycling-symbol"
          else "objects.wrench"
        case _: AppealMsg       => "symbols.left-speech-bubble"
        case _: Note            => "objects.label"
        case _: ReportNewAtom   => "symbols.exclamation-mark"
        case _: PlayBans        => "objects.hourglass-not-done"
        case _: ReportLineFlag  => "symbols.exclamation-mark"
        case _: AccountCreation => "food-drink.egg" // how is egg in "food" instead of "animal"
    def at: Instant = e match
      case e: Modlog               => e.date
      case e: AppealMsg            => e.at
      case e: Note                 => e.date
      case ReportNewAtom(_, atoms) => atoms.head.at
      case e: PlayBans             => e.list.head.date
      case e: ReportLineFlag       => e.line.date
      case AccountCreation(at)     => at
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
      case _: PlayBans                                     => angle != Angle.Comm
      case l: Modlog if l.action == Modlog.chatTimeout     => angle != Angle.Play
      case l: Modlog if l.action == Modlog.deletePost      => angle != Angle.Play
      case l: Modlog if l.action == Modlog.disableTeam     => angle != Angle.Play
      case l: Modlog if l.action == Modlog.teamKick        => angle != Angle.Play
      case l: Modlog if l.action == Modlog.blankedPassword => angle == Angle.None
      case l: Modlog if l.action == Modlog.weakPassword    => angle == Angle.None
      case l: Modlog if l.action == Modlog.troll           => angle != Angle.Play
      case l: Modlog if l.action == Modlog.modMessage      =>
        angle match
          case Comm => !l.details.has(lila.playban.PlaybanFeedback.sittingAutoPreset.name)
          case _    => true
      case r: ReportNewAtom if r.report.is(_.Comm) => angle != Angle.Play
      case r: ReportLineFlag                       => r.openId.isDefined || angle == Angle.Comm
      case _                                       => true

final class ModTimelineApi(
    modLogApi: ModlogApi,
    appealApi: AppealApi,
    noteApi: NoteApi,
    reportApi: ReportApi,
    playBanApi: PlaybanApi,
    userRepo: lila.user.UserRepo
)(using Executor)(using scheduler: Scheduler):

  def load(user: User, withPlayBans: Boolean)(using me: Me): Fu[ModTimeline] =
    for
      modLogAll <- Granter(_.ModLog).so(modLogApi.userHistory(user.id))
      modLog = modLogAll.filter(filterModLog)
      appeal   <- Granter(_.Appeals).so(appealApi.byId(user))
      notesAll <- noteApi.getForMyPermissions(user, Max(50))
      notes       = notesAll.filter(filterNote)
      loadReports = Granter(_.SeeReport) && me.isnt(user)
      reportsAll <- loadReports.so(reportApi.allReportsAbout(user, Max(50)))
      reports = reportsAll.filter(filterReport)
      playban <- withPlayBans.so(Granter(_.SeeReport)).so(playBanApi.fetchRecord(user))
    yield ModTimeline(user, modLog, appeal, notes, reports, playban)

  private def filterModLog(l: Modlog): Boolean =
    if l.action == Modlog.teamKick && !modsList.contains(l.mod) then false
    else if l.action == Modlog.teamEdit && !modsList.contains(l.mod) then false
    else true

  private def filterNote(note: Note): Boolean =
    if note.from.is(UserId.irwin) then false
    else if note.text.startsWith("Appeal reply:") then false
    else true

  private def filterReport(r: Report): Boolean =
    !r.isSpontaneous && !r.isAppeal

  private object modsList:
    var all: Set[ModId]               = Set(UserId.lichess.into(ModId))
    def contains(mod: ModId): Boolean = all.contains(mod)
    scheduler.scheduleWithFixedDelay(19.seconds, 1.hour): () =>
      userRepo
        .userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList)
        .foreach: ids =>
          all = ids.map(_.into(ModId)).toSet
