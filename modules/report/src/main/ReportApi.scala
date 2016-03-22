package lila.report

import akka.actor.ActorSelection
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, UserRepo }
import tube.reportTube

private[report] final class ReportApi {

  def create(setup: ReportSetup, by: User): Funit = !by.troll ?? {
    Reason(setup.reason).fold[Funit](fufail(s"Invalid report reason ${setup.reason}")) { reason =>
      val user = setup.user
      val report = Report.make(
        user = setup.user,
        reason = reason,
        text = setup.text,
        createdBy = by)
      !isAlreadySlain(report, user) ?? {
        lila.mon.mod.report.create(reason.name)()
        if (by.id == UserRepo.lichessId) reportTube.coll.update(
          selectRecent(user, reason),
          Json.obj("$set" -> (reportTube.toMongo(report).get - "processedBy" - "_id"))
        ) flatMap { res =>
            (res.n == 0) ?? $insert(report)
          }
        else $insert(report)
      }
    } >>- monitorUnprocessed
  }

  private def monitorUnprocessed = nbUnprocessed foreach { nb =>
    lila.mon.mod.report.unprocessed(nb)
  }

  private def isAlreadySlain(report: Report, user: User) =
    (report.isCheat && user.engine) ||
      (report.isAutomatic && report.isOther && user.troll) ||
      (report.isTrollOrInsult && user.troll)

  def autoCheatPrintReport(userId: String): Funit = {
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "cheatprint",
        text = "Shares print with known cheaters",
        gameId = "",
        move = ""), lichess)
      case _ => funit
    }
  }

  def autoCheatReport(userId: String, text: String): Funit = {
    lila.mon.cheat.autoReport.count()
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

  def autoBotReport(userId: String, referer: Option[String]): Funit = {
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "cheat",
        text = s"""Python bot detected on ${referer | "?"}""",
        gameId = "",
        move = ""), lichess)
      case _ => funit
    }
  }

  def autoBoostReport(userId: String, accompliceId: String): Funit = {
    UserRepo.byId(userId) zip
      UserRepo.byId(accompliceId) zip
      UserRepo.lichess flatMap {
        case ((Some(user), Some(accomplice)), Some(lichess)) => create(ReportSetup(
          user = user,
          reason = "boost",
          text = s"with their accomplice @${accomplice.username}",
          gameId = "",
          move = ""), lichess)
        case _ => funit
      }
  }

  def clean(userId: String): Funit = $update(
    Json.obj(
      "user" -> userId,
      "reason" -> "cheat"
    ) ++ unprocessedSelect,
    $set("processedBy" -> "lichess"),
    multi = true)

  def process(id: String, by: User): Funit = $find byId id flatMap {
    _ ?? { report =>
      $update(
        Json.obj(
          "user" -> report.user,
          "reason" -> report.reason
        ) ++ unprocessedSelect,
        $set("processedBy" -> by.id),
        multi = true)
    } >>- monitorUnprocessed >>- lila.mon.mod.report.close()
  }

  def processEngine(userId: String, byModId: String): Funit = $update(
    Json.obj(
      "user" -> userId,
      "reason" -> $in(List(Reason.Cheat.name, Reason.CheatPrint.name))
    ) ++ unprocessedSelect,
    $set("processedBy" -> byModId),
    multi = true) >>- monitorUnprocessed

  def processTroll(userId: String, byModId: String): Funit = $update(
    Json.obj(
      "user" -> userId,
      "reason" -> $in(List(Reason.Insult.name, Reason.Troll.name, Reason.Other.name))
    ) ++ unprocessedSelect,
    $set("processedBy" -> byModId),
    multi = true) >>- monitorUnprocessed

  def autoInsultReport(userId: String, text: String): Funit = {
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "insult",
        text = text,
        gameId = "",
        move = ""), lichess)
      case _ => funit
    }
  } >>- monitorUnprocessed

  def autoProcess(userId: String): Funit =
    $update(
      Json.obj("user" -> userId.toLowerCase),
      Json.obj("processedBy" -> "lichess")) >>- monitorUnprocessed

  private val unprocessedSelect = Json.obj("processedBy" -> $exists(false))
  private val processedSelect = Json.obj("processedBy" -> $exists(true))

  def nbUnprocessed = $count(unprocessedSelect)

  def recent(nb: Int) = $find($query.all sort $sort.createdDesc, nb)

  def unprocessedAndRecent(nb: Int): Fu[List[Report.WithUser]] =
    recentUnprocessed(nb) |+| recentProcessed(nb) flatMap { all =>
      val reports = all take nb
      UserRepo byIds reports.map(_.user).distinct map { users =>
        reports.flatMap { r =>
          users.find(_.id == r.user) map { Report.WithUser(r, _) }
        }
      }
    }

  def recentUnprocessed(nb: Int) =
    $find($query(unprocessedSelect) sort $sort.createdDesc, nb)

  def recentProcessed(nb: Int) = $find($query(processedSelect) sort $sort.createdDesc, nb)

  private def selectRecent(user: User, reason: Reason) = Json.obj(
    "createdAt" -> $gt($date(DateTime.now minusDays 7)),
    "user" -> user.id,
    "reason" -> reason.name)

  private def findRecent(user: User, reason: Reason): Fu[Option[Report]] =
    $find.one(selectRecent(user, reason))
}
