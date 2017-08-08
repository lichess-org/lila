package lila.mod

import play.api.libs.json._

import lila.common.LightUser
import lila.report.{ Report, ReportApi }
import lila.user.{ User, UserRepo, Note, NoteApi }

case class Inquiry(
    mod: LightUser,
    report: Report,
    accuracy: Option[Int],
    moreReports: List[Report],
    notes: List[Note],
    user: User
) {

  def allReports = report :: moreReports
}

final class InquiryApi(
    reportApi: ReportApi,
    noteApi: NoteApi
) {

  def forMod(mod: User): Fu[Option[Inquiry]] =
    lila.security.Granter(_.Hunter)(mod).?? {
      reportApi.inquiries.ofModId(mod.id).flatMap {
        _ ?? { report =>
          reportApi.moreLike(report, 10) zip
            UserRepo.named(report.user) zip
            reportApi.accuracy(report) zip
            noteApi.forMod(report.user) map {
              case moreReports ~ userOption ~ accuracy ~ notes =>
                userOption ?? { user =>
                  Inquiry(mod.light, report, accuracy, moreReports, notes, user).some
                }
            }
        }
      }
    }

  import lila.common.LightUser.lightUserWrites
  import lila.report.JsonView.reportWrites
  import lila.user.JsonView.modWrites
  // val inquiryWrites = Json.writes[Inquiry]
}
