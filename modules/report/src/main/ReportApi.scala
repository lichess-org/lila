package lila.report

import akka.actor.ActorSelection
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, UserRepo }
import tube.reportTube

final class ReportApi(evaluator: ActorSelection) {

  def create(setup: ReportSetup, by: User): Funit =
    Reason(setup.reason).fold[Funit](fufail("Invalid report reason " + setup.reason)) { reason ⇒
      val user = setup.user
      val report = Report.make(
        user = setup.user,
        reason = reason,
        text = setup.text,
        createdBy = by)
      (!report.isCheat || !user.engine) ?? {
        $insert(report) >>- {
          if (report.isCheat && report.isManual) evaluator ! user
        }
      }
    }

  def autoCheatReport(userId: String, text: String): Funit =
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) ⇒ create(ReportSetup(
        user = user,
        reason = "cheat",
        text = text,
        gameId = "",
        move = ""), lichess)
      case _ ⇒ funit
    }

  def process(id: String, by: User): Funit =
    $update.field(id, "processedBy", by.id)

  def autoProcess(userId: String): Funit =
    $update(Json.obj("user" -> userId.toLowerCase), Json.obj("processedBy" -> "lichess"))

  def nbUnprocessed = $count(Json.obj("processedBy" -> $exists(false)))

  def recent = $find($query.all sort $sort.createdDesc, 50)
}
