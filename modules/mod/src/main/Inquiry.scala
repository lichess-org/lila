package lila.mod

import lila.core.LightUser
import lila.report.{ Report, ReportApi }
import lila.user.{ Me, Note, NoteApi, User, UserApi }

case class Inquiry(
    mod: LightUser,
    report: Report,
    moreReports: List[Report],
    notes: List[Note],
    history: List[lila.mod.Modlog],
    user: User.WithPerfs
):
  def allReports = report :: moreReports
  def alreadyMarked =
    (report.isCheat && user.marks.engine) ||
      (report.isBoost && user.marks.boost) ||
      (report.isComm && user.marks.troll)

final class InquiryApi(
    userApi: UserApi,
    reportApi: ReportApi,
    noteApi: NoteApi,
    logApi: ModlogApi
):

  def forMod(using mod: Me)(using Executor): Fu[Option[Inquiry]] =
    lila.security.Granter(_.SeeReport).so {
      reportApi.inquiries
        .ofModId(mod)
        .flatMapz: report =>
          (
            reportApi.moreLike(report, 10),
            userApi.withPerfs(report.user),
            noteApi.byUserForMod(report.user),
            logApi.userHistory(report.user)
          ).mapN: (moreReports, userOption, notes, history) =>
            userOption.map: user =>
              Inquiry(mod.light, report, moreReports, notes, history, user)
    }
