package lila.report

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo, NoteApi }

final class ReportApi(
    val coll: Coll,
    noteApi: NoteApi,
    isOnline: User.ID => Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    bus: lila.common.Bus
) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val ReasonBSONHandler = isoHandler[Reason, String, BSONString](Reason.reasonIso)
  private implicit val InquiryBSONHandler = Macros.handler[Inquiry]
  private implicit val ReportBSONHandler = Macros.handler[Report]

  def create(setup: ReportSetup, by: User): Funit = create(Report.make(
    user = setup.user,
    reason = Reason(setup.reason).err(s"Invalid report reason ${setup.reason}"),
    text = setup.text,
    createdBy = by
  ), setup.user, by)

  def create(report: Report, reported: User, by: User): Funit = !by.troll ?? {
    !isAlreadySlain(report, reported) ?? {

      lila.mon.mod.report.create(report.reason.key)()

      def insert = coll.insert(report).void >>-
        bus.publish(lila.hub.actorApi.report.Created(reported.id, report.reason.key), 'report)

      if (by.id == UserRepo.lichessId) coll.update(
        selectRecent(reported, report.reason),
        $doc("$set" -> ReportBSONHandler.write(report).remove("processedBy", "_id"))
      ) flatMap { res => (res.n == 0) ?? insert }
      else insert
    } >>- monitorUnprocessed
  }

  private def monitorUnprocessed = {
    nbUnprocessedCache.refresh
    nbUnprocessed foreach { nb =>
      lila.mon.mod.report.unprocessed(nb)
    }
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
        move = ""
      ), lichess)
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
        move = ""
      ), lichess)
      case _ => funit
    }
  }

  def autoBotReport(userId: String, referer: Option[String], name: String): Funit = {
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "cheat",
        text = s"""$name bot detected on ${referer | "?"}""",
        gameId = "",
        move = ""
      ), lichess)
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
          move = ""
        ), lichess)
        case _ => funit
      }
  }

  private def publishProcessed(userId: User.ID, reason: Reason) =
    bus.publish(lila.hub.actorApi.report.Processed(userId, reason.key), 'report)

  def clean(userId: User.ID): Funit = coll.update(
    $doc(
      "user" -> userId,
      "reason" -> Reason.Cheat.key
    ) ++ unprocessedSelect,
    $set("processedBy" -> "lichess"),
    multi = true
  ).void >>- publishProcessed(userId, Reason.Cheat)

  def process(id: String, by: User): Funit = coll.byId[Report](id) flatMap {
    _ ?? { report =>
      coll.update(
        $doc(
          "user" -> report.user,
          "reason" -> report.reason,
          "processedBy" $exists false
        ),
        $set("processedBy" -> by.id) ++ $unset("inquiry"),
        multi = true
      ).void >>- {
          monitorUnprocessed
          lila.mon.mod.report.close()
          publishProcessed(report.user, report.reason)
        }
    }
  }

  def processEngine(userId: String, byModId: String): Funit = coll.update(
    $doc(
      "user" -> userId,
      "reason" $in List(Reason.Cheat.key, Reason.CheatPrint.key),
      "processedBy" $exists false
    ),
    $set("processedBy" -> byModId) ++ $unset("inquiry"),
    multi = true
  ).void >>- {
      monitorUnprocessed
      publishProcessed(userId, Reason.Cheat)
    }

  def processTroll(userId: String, byModId: String): Funit = coll.update(
    $doc(
      "user" -> userId,
      "reason" $in List(Reason.Insult.key, Reason.Troll.key, Reason.Other.key),
      "processedBy" $exists false
    ),
    $set("processedBy" -> byModId) ++ $unset("inquiry"),
    multi = true
  ).void >>- monitorUnprocessed

  def autoInsultReport(userId: String, text: String): Funit = {
    UserRepo byId userId zip UserRepo.lichess flatMap {
      case (Some(user), Some(lichess)) => create(ReportSetup(
        user = user,
        reason = "insult",
        text = text,
        gameId = "",
        move = ""
      ), lichess)
      case _ => funit
    }
  } >>- monitorUnprocessed

  private val unprocessedSelect: Bdoc = $doc(
    "processedBy" $exists false,
    "inquiry" $exists false
  )
  private val processedSelect: Bdoc = "processedBy" $exists true
  private def reasonSelect(reason: Option[Reason]): Bdoc =
    reason.?? { r => $doc("reason" -> r.key) }

  val nbUnprocessedCache = asyncCache.single[Int](
    name = "report.nbUnprocessed",
    f = coll.countSel(unprocessedSelect),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )

  def nbUnprocessed = nbUnprocessedCache.get

  def recent(user: User, nb: Int, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Report]] =
    coll.find($doc("user" -> user.id)).sort($sort.createdDesc).list[Report](nb)

  def byAndAbout(user: User, nb: Int): Fu[Report.ByAndAbout] = for {
    by <- coll.find($doc("createdBy" -> user.id)).sort($sort.createdDesc).list[Report](nb, ReadPreference.secondaryPreferred)
    about <- recent(user, nb, ReadPreference.secondaryPreferred)
  } yield Report.ByAndAbout(by, about)

  def recentReportersOf(user: User): Fu[List[User.ID]] =
    coll.distinctWithReadPreference[String, List]("createdBy", $doc(
      "user" -> user.id,
      "createdAt" $gt DateTime.now.minusDays(3),
      "createdBy" $ne "lichess"
    ).some,
      ReadPreference.secondaryPreferred)

  def unprocessedAndRecentWithFilter(nb: Int, reason: Option[Reason]): Fu[List[Report.WithUserAndNotes]] = for {
    unprocessed <- findRecent(nb, unprocessedSelect ++ reasonSelect(reason))
    processed <- findRecent(nb - unprocessed.size, processedSelect ++ reasonSelect(reason))
    withNotes <- addUsersAndNotes(unprocessed ++ processed)
  } yield withNotes

  def unprocessedWithFilter(nb: Int, reason: Option[Reason]): Fu[List[Report.WithUserAndNotes]] =
    findRecent(nb, unprocessedSelect ++ reasonSelect(reason)) flatMap addUsersAndNotes

  private def addUsersAndNotes(reports: List[Report]): Fu[List[Report.WithUserAndNotes]] = for {
    withUsers <- UserRepo byIdsSecondary reports.map(_.user).distinct map { users =>
      reports.flatMap { r =>
        users.find(_.id == r.user) map { u =>
          Report.WithUser(r, u, isOnline(u.id))
        }
      }
    }
    sorted = withUsers.sortBy(-_.urgency)
    withNotes <- noteApi.byMod(sorted.map(_.user.id).distinct) map { notes =>
      sorted.map { wu =>
        Report.WithUserAndNotes(wu, notes.filter(_.to == wu.user.id))
      }
    }
  } yield withNotes

  def countUnprocesssedByReasons: Fu[Map[Reason, Int]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateWithReadPreference(
      Match(unprocessedSelect),
      List(
        GroupField("reason")("nb" -> SumValue(1))
      ),
      ReadPreference.secondaryPreferred
    ).map {
        _.firstBatch.flatMap { doc =>
          doc.getAs[String]("_id") flatMap Reason.apply flatMap { reason =>
            doc.getAs[Int]("nb") map { reason -> _ }
          }
        }(scala.collection.breakOut)
      }
  }

  def currentlyReportedForCheat: Fu[Set[User.ID]] =
    coll.distinctWithReadPreference[User.ID, Set](
      "user",
      Some($doc("reason" -> Reason.Cheat.key) ++ unprocessedSelect),
      ReadPreference.secondaryPreferred
    )

  object inquiries {

    def all: Fu[List[Report]] = coll.list[Report]($doc("inquiry.mod" $exists true))

    def ofModId(modId: User.ID): Fu[Option[Report]] = coll.uno[Report]($doc("inquiry.mod" -> modId))
  }

  private def findRecent(nb: Int, selector: Bdoc) =
    coll.find(selector).sort($sort.createdDesc).list[Report](nb)

  private def selectRecent(user: User, reason: Reason): Bdoc = $doc(
    "createdAt" $gt DateTime.now.minusDays(7),
    "user" -> user.id,
    "reason" -> reason.key
  )
}
