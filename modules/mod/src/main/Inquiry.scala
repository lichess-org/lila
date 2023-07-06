package lila.mod

import lila.common.LightUser
import lila.report.{ Report, ReportApi }
import lila.user.{ Note, NoteApi, User, UserApi, Me }

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
      reportApi.inquiries.ofModId(mod).flatMapz { report =>
        reportApi.moreLike(report, 10) zip
          userApi.withPerfs(report.user) zip
          noteApi.byUserForMod(report.user) zip
          logApi.userHistory(report.user) map { case (((moreReports, userOption), notes), history) =>
            userOption.map: user =>
              Inquiry(mod.light, report, moreReports, notes, history, user)
          }
      }
    }
