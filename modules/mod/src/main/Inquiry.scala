package lila.mod

import lila.core.LightUser
import lila.core.perf.UserWithPerfs
import lila.report.{ Report, ReportApi }
import lila.user.{ Me, Note, NoteApi, UserApi }

case class Inquiry(
    mod: LightUser,
    report: Report,
    moreReports: List[Report],
    notes: List[Note],
    history: List[lila.mod.Modlog],
    user: UserWithPerfs
):
  def allReports = report :: moreReports
  def alreadyMarked =
    (report.is(_.Cheat) && user.marks.engine) ||
      (report.is(_.Boost) && user.marks.boost) ||
      (report.is(_.Comm) && user.marks.troll)

final class InquiryApi(
    userApi: UserApi,
    reportApi: ReportApi,
    noteApi: NoteApi,
    logApi: ModlogApi
):
  def forMod(using mod: Me)(using Executor): Fu[Option[Inquiry]] =
    lila.core.perm
      .Granter(_.SeeReport)
      .so:
        reportApi.inquiries
          .ofModId(mod)
          .flatMapz: report =>
            (
              reportApi.moreLike(report, Max(10)),
              userApi.withPerfs(report.user),
              noteApi.toUserForMod(report.user),
              logApi.userHistory(report.user)
            ).mapN: (moreReports, userOption, notes, history) =>
              userOption.map: user =>
                Inquiry(mod.light, report, moreReports, notes, history, user)
