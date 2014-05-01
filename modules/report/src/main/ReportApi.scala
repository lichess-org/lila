package lila.report

import akka.actor.ActorSelection
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, UserRepo }
import tube.reportTube

private[report] final class ReportApi(evaluator: ActorSelection) {

  def create(setup: ReportSetup, by: User, update: Boolean = false): Funit =
    Reason(setup.reason).fold[Funit](fufail("Invalid report reason " + setup.reason)) { reason =>
      val user = setup.user
      val report = Report.make(
        user = setup.user,
        reason = reason,
        text = setup.text,
        createdBy = by)
      (!report.isCheat || !user.engine) ?? {
        findRecent(user, reason) flatMap {
          case Some(existing) if update =>
            $update($select(existing.id), $set("text" -> report.text))
          case Some(_) =>
            logger.info(s"skip existing report creation: $reason $user")
            funit
          case None => $insert(report) >>- {
            if (report.isCheat && report.isManual) evaluator ! user
          }
        }
      }
    }

  def autoCheatReport(userId: String, text: String): Funit = {
    logger.info(s"auto cheat reaport $userId: ${~text.lines.toList.headOption}")
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "cheat",
        text = text,
        gameId = "",
        move = ""), lichess, update = true)
      case _ => funit
    }
  }

  def process(id: String, by: User): Funit =
    $update.field(id, "processedBy", by.id)

  def autoProcess(userId: String): Funit =
    $update(Json.obj("user" -> userId.toLowerCase), Json.obj("processedBy" -> "lichess"))

  def nbUnprocessed = $count(Json.obj("processedBy" -> $exists(false)))

  def recent = $find($query.all sort $sort.createdDesc, 50)

  private def findRecent(user: User, reason: Reason): Fu[Option[Report]] =
    $find.one(Json.obj(
      "createdAt" -> $gt($date(DateTime.now minusDays 3)),
      "user" -> user.id,
      "reason" -> reason.name))

  private val logger = play.api.Logger("report")
}
