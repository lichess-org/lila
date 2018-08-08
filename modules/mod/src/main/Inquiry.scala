package lidraughts.mod

import lidraughts.common.LightUser
import lidraughts.report.{ Report, ReportApi }
import lidraughts.user.{ User, UserRepo, Note, NoteApi }

case class Inquiry(
    mod: LightUser,
    report: Report,
    moreReports: List[Report],
    notes: List[Note],
    history: List[lidraughts.mod.Modlog],
    user: User
) {

  def allReports = report :: moreReports
}

final class InquiryApi(
    reportApi: ReportApi,
    noteApi: NoteApi,
    logApi: ModlogApi
) {

  def forMod(mod: User): Fu[Option[Inquiry]] =
    lidraughts.security.Granter(_.Hunter)(mod).?? {
      reportApi.inquiries.ofModId(mod.id).flatMap {
        _ ?? { report =>
          reportApi.moreLike(report, 10) zip
            UserRepo.named(report.user) zip
            noteApi.forMod(report.user) zip
            logApi.userHistory(report.user) map {
              case moreReports ~ userOption ~ notes ~ history =>
                userOption ?? { user =>
                  Inquiry(mod.light, report, moreReports, notes, history, user).some
                }
            }
        }
      }
    }
}
