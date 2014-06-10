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
      !isAlreadySlayed(report, user) ?? {
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

  private def isAlreadySlayed(report: Report, user: User) =
    (report.isCheat && user.engine) ||
      (report.isAutomatic && report.isOther && user.troll) ||
      (report.isTroll && user.troll)

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

  def autoBlockReport(userId: String, blocked: Int, followed: Int): Funit = {
    logger.info(s"auto block reaport $userId: $blocked blockers & $followed followers")
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "other",
        text = s"[AUTOREPORT] Blocked $blocked times, followed by $followed players",
        gameId = "",
        move = ""), lichess)
      case _ => funit
    }
  }

  def process(id: String, by: User): Funit =
    $update.field(id, "processedBy", by.id)

  def autoProcess(userId: String): Funit =
    $update(Json.obj("user" -> userId.toLowerCase), Json.obj("processedBy" -> "lichess"))

  private val unprocessedSelect = Json.obj("processedBy" -> $exists(false))
  private val processedSelect = Json.obj("processedBy" -> $exists(true))

  def nbUnprocessed = $count(unprocessedSelect)

  def recent(nb: Int) = $find($query.all sort $sort.createdDesc, nb)

  def unprocessedAndRecent(nb: Int) = recentUnprocessed |+| recentProcessed(nb)

  def recentUnprocessed = $find($query(unprocessedSelect) sort $sort.createdDesc)

  def recentProcessed(nb: Int) = $find($query(processedSelect) sort $sort.createdDesc, nb)

  private def findRecent(user: User, reason: Reason): Fu[Option[Report]] =
    $find.one(Json.obj(
      "createdAt" -> $gt($date(DateTime.now minusDays 3)),
      "user" -> user.id,
      "reason" -> reason.name))

  private val logger = play.api.Logger("report")
}
