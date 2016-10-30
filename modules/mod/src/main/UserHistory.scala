package lila.mod

import play.api.libs.json._

import lila.report.Report
import lila.user.User

final class UserHistory(
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi) {

  import JsonView.modlogWrites
  import lila.report.JsonView.reportWrites

  def apply(user: User): Fu[JsArray] = for {
    logs <- logApi.userHistory(user.id)
    reports <- reportApi.recent(user, 15)
  } yield {
    val all: List[Either[Modlog, Report]] = logs.map(Left.apply) ::: reports.map(Right.apply)
    val sorted = all.sortBy {
      case Left(log)  => -log.date.getMillis
      case Right(rep) => -rep.createdAt.getMillis
    }
    JsArray {
      sorted map {
        case Left(log) => Json.obj(
          "type" -> "modAction",
          "data" -> (modlogWrites writes log))
        case Right(rep) => Json.obj(
          "type" -> "report",
          "data" -> (reportWrites writes rep))
      }
    }
  }
}
