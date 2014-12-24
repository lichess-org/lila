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

  def create(setup: ReportSetup, by: User): Funit =
    Reason(setup.reason).fold[Funit](fufail("Invalid report reason " + setup.reason)) { reason =>
      val user = setup.user
      val report = Report.make(
        user = setup.user,
        reason = reason,
        text = setup.text,
        createdBy = by)
      !isAlreadySlain(report, user) ?? {
        if (by.id == UserRepo.lichessId) reportTube.coll.update(
          selectRecent(user, reason),
          reportTube.toMongo(report).get - "_id"
        ) flatMap { res =>
            (!res.updatedExisting) ?? {
              if (report.isCheat) evaluator ! user
              $insert(report)
            }
          }
        else {
          if (report.isCheat) evaluator ! user
          $insert(report)
        }
      }
    }

  private def isAlreadySlain(report: Report, user: User) =
    (report.isCheat && user.engine) ||
      (report.isAutomatic && report.isOther && user.troll) ||
      (report.isTroll && user.troll)

  def autoCheatReport(userId: String, text: String): Funit = {
    logger.info(s"auto cheat report $userId: ${~text.lines.toList.headOption}")
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "cheat",
        text = text,
        gameId = "",
        move = ""), lichess)
      case _ => funit
    }
  }

  def process(id: String, by: User): Funit = $find byId id flatMap {
    _ ?? { report =>
      $update(
        Json.obj(
          "user" -> report.user,
          "reason" -> report.reason
        ) ++ unprocessedSelect,
        $set("processedBy" -> by.id),
        multi = true)
    }
  }

  def autoProcess(userId: String): Funit =
    $update(Json.obj("user" -> userId.toLowerCase), Json.obj("processedBy" -> "lichess"))

  private val unprocessedSelect = Json.obj("processedBy" -> $exists(false))
  private val processedSelect = Json.obj("processedBy" -> $exists(true))

  def nbUnprocessed = $count(unprocessedSelect)

  def recent(nb: Int) = $find($query.all sort $sort.createdDesc, nb)

  def unprocessedAndRecent(nb: Int) = recentUnprocessed |+| recentProcessed(nb)

  def recentUnprocessed = $find($query(unprocessedSelect) sort $sort.createdDesc)

  def recentProcessed(nb: Int) = $find($query(processedSelect) sort $sort.createdDesc, nb)

  private def selectRecent(user: User, reason: Reason) = Json.obj(
    "createdAt" -> $gt($date(DateTime.now minusDays 3)),
    "user" -> user.id,
    "reason" -> reason.name)

  private def findRecent(user: User, reason: Reason): Fu[Option[Report]] =
    $find.one(selectRecent(user, reason))

  private val logger = play.api.Logger("report")
}
