package lila.api

import lila.mod.{ Modlog, ModlogApi }
import lila.appeal.{ Appeal, AppealMsg, AppealApi }
import lila.user.{ Note, NoteApi }
import lila.report.{ Report, ReportApi }
import lila.playban.{ TempBan, PlaybanApi }

case class ModTimeline(
    user: User,
    modLog: List[Modlog],
    appeal: Option[Appeal],
    notes: List[Note],
    reports: Report.ByAndAbout,
    playban: lila.playban.UserRecord
):
  import ModTimeline.{ *, given }

  def atoms: List[Report.Atom] = reports.all.flatMap(_.atomsByAndAbout(user.id))

  def chronological: List[Event] =
    val all: List[Event] =
      modLog ::: appeal.so(_.msgs.toList) ::: notes ::: atoms ::: playban.bans.toList
    all.sorted

  def commReportsAbout: List[Report] = reports.about.filter(_.is(_.Comm))

object ModTimeline:

  type Event = Modlog | AppealMsg | Note | Report.Atom | TempBan

  val dateOf: Event => Instant =
    case e: Modlog      => e.date
    case e: AppealMsg   => e.at
    case e: Note        => e.date
    case e: Report.Atom => e.at
    case e: TempBan     => e.date

  given Ordering[Event] = Ordering.by(dateOf)

final class ModTimelineApi(
    modLogApi: ModlogApi,
    appealApi: AppealApi,
    noteApi: NoteApi,
    reportApi: ReportApi,
    playBanApi: PlaybanApi
)(using Executor):

  def apply(user: User)(using Me): Fu[ModTimeline] =
    for
      modLog  <- modLogApi.userHistory(user.id)
      appeal  <- appealApi.byId(user)
      notes   <- noteApi.get(user)
      reports <- reportApi.byAndAbout(user, Max(50))
      playban <- playBanApi.fetchRecord(user)
    yield ModTimeline(user, modLog, appeal, notes, reports, playban)
