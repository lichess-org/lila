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
  private implicit val RoomBSONHandler = isoHandler[Room, String, BSONString](Room.roomIso)
  import Report.Inquiry
  private implicit val InquiryBSONHandler = Macros.handler[Inquiry]
  private implicit val ReportBSONHandler = Macros.handler[Report]

  def create(setup: ReportSetup, by: User): Funit = create(Report.make(
    user = setup.user,
    reason = Reason(setup.reason).err(s"Invalid report reason ${setup.reason}"),
    text = setup.text,
    createdBy = by
  ), setup.user, by)

  def create(report: Report, reported: User, by: User): Funit = !by.reportban ?? {
    !isAlreadySlain(report, reported) ?? {

      lila.mon.mod.report.create(report.reason.key)()

      def insert = coll.insert(report).void >>-
        bus.publish(lila.hub.actorApi.report.Created(reported.id, report.reason.key, by.id), 'report)

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
          "reason" -> report.reason
        ),
        $set("processedBy" -> by.id) ++ $unset("inquiry"),
        multi = true
      ).void >>- {
          monitorUnprocessed
          lila.mon.mod.report.close()
          publishProcessed(report.user, report.reason)
          accuracyCache invalidate report.createdBy
        }
    }
  }

  def processEngine(userId: String, byModId: String): Funit = coll.update(
    $doc(
      "user" -> userId,
      "reason" $in List(Reason.Cheat.key, Reason.CheatPrint.key),
      "processedBy" $exists false
    ),
    $set("processedBy" -> byModId),
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
    $set("processedBy" -> byModId),
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

  def moveToXfiles(id: String): Funit = coll.update(
    $id(id),
    $set("room" -> Room.Xfiles.key) ++ $unset("inquiry")
  ).void

  private val unprocessedSelect: Bdoc = $doc(
    "processedBy" $exists false,
    "inquiry" $exists false
  )
  private val processedSelect: Bdoc = "processedBy" $exists true
  private def roomSelect(room: Option[Room]): Bdoc =
    room.fold($doc("room" $ne Room.Xfiles.key)) { r => $doc("room" -> r) }

  val nbUnprocessedCache = asyncCache.single[Int](
    name = "report.nbUnprocessed",
    f = coll.countSel(unprocessedSelect ++ roomSelect(none)),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )

  def nbUnprocessed = nbUnprocessedCache.get

  def recent(user: User, nb: Int, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Report]] =
    coll.find($doc("user" -> user.id)).sort($sort.createdDesc).list[Report](nb, readPreference)

  def moreLike(report: Report, nb: Int): Fu[List[Report]] =
    coll.find($doc("user" -> report.user, "_id" $ne report.id)).sort($sort.createdDesc).list[Report](nb)

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

  def unprocessedAndRecentWithFilter(nb: Int, room: Option[Room]): Fu[List[Report.WithUserAndNotes]] = for {
    unprocessed <- findRecent(nb, unprocessedSelect ++ roomSelect(room))
    nbProcessed = nb - unprocessed.size
    processed <- if (room.has(Room.Xfiles) || nbProcessed == 0) fuccess(Nil)
    else findRecent(nbProcessed, processedSelect ++ roomSelect(room))
    withNotes <- addUsersAndNotes(unprocessed ++ processed)
  } yield withNotes

  def unprocessedWithFilter(nb: Int, room: Option[Room]): Fu[List[Report.WithUserAndNotes]] =
    findRecent(nb, unprocessedSelect ++ roomSelect(room)) flatMap addUsersAndNotes

  private def addUsersAndNotes(reports: List[Report]): Fu[List[Report.WithUserAndNotes]] = for {
    withUsers <- UserRepo byIdsSecondary reports.map(_.user).distinct map { users =>
      reports.flatMap { r =>
        users.find(_.id == r.user) map { u =>
          accuracy(r) map { a =>
            Report.WithUser(r, u, isOnline(u.id), a)
          }
        }
      }
    }
    sorted <- withUsers.sequenceFu map { wu =>
      wu.sortBy(-_.urgency)
    }
    withNotes <- noteApi.byMod(sorted.map(_.user.id).distinct) map { notes =>
      sorted.map { wu =>
        Report.WithUserAndNotes(wu, notes.filter(_.to == wu.user.id))
      }
    }
  } yield withNotes

  private val accuracyCache = asyncCache.clearable[User.ID, Int](
    name = "reporterAccuracy",
    f = accuracyForUser,
    expireAfter = _.ExpireAfterWrite(1 hours)
  )

  private def accuracyForUser(reporterId: User.ID): Fu[Int] = for {
    reports <- coll.find($doc(
      "createdBy" -> reporterId,
      "reason" -> Reason.Cheat.key,
      "processedBy" $exists true
    ))
      .sort($sort.createdDesc)
      .list[Report](20, ReadPreference.secondaryPreferred)
    userIds = reports.map(_.user).distinct
    nbEngines <- UserRepo countEngines userIds
  } yield Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100)

  def accuracy(report: Report): Fu[Option[Int]] =
    (report.reason == Reason.Cheat && !report.processed) ?? {
      accuracyCache get report.createdBy map some
    }

  def countUnprocesssedByRooms: Fu[Room.Counts] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregate(
      Match(unprocessedSelect),
      List(
        GroupField("room")("nb" -> SumValue(1))
      )
    ).map { res =>
        Room.Counts(res.firstBatch.flatMap { doc =>
          doc.getAs[String]("_id") flatMap Room.apply flatMap { room =>
            doc.getAs[Int]("nb") map { room -> _ }
          }
        }.toMap)
      }
  }

  def currentlyReportedForCheat: Fu[Set[User.ID]] =
    coll.distinctWithReadPreference[User.ID, Set](
      "user",
      Some($doc("reason" -> Reason.Cheat.key) ++ unprocessedSelect),
      ReadPreference.secondaryPreferred
    )

  private def findRecent(nb: Int, selector: Bdoc) =
    coll.find(selector).sort($sort.createdDesc).list[Report](nb)

  private def selectRecent(user: User, reason: Reason): Bdoc = $doc(
    "createdAt" $gt DateTime.now.minusDays(7),
    "user" -> user.id,
    "reason" -> reason.key
  )

  object inquiries {

    def all: Fu[List[Report]] = coll.list[Report]($doc("inquiry.mod" $exists true))

    def ofModId(modId: User.ID): Fu[Option[Report]] = coll.uno[Report]($doc("inquiry.mod" -> modId))

    def toggle(mod: User, id: String): Fu[Option[Report]] = for {
      report <- coll.byId[Report](id) flatten s"No report $id found"
      current <- ofModId(mod.id)
      _ <- current ?? cancel(mod)
      isSame = current.exists(_.id == report.id)
      _ <- !isSame ?? coll.updateField(
        $id(report.id),
        "inquiry",
        Report.Inquiry(mod.id, DateTime.now)
      ).void
    } yield !isSame option report

    def cancel(mod: User)(report: Report): Funit =
      if (report.isOther && report.createdBy == mod.id) coll.remove($id(report.id)).void
      else coll.update(
        $id(report.id),
        $unset("inquiry", "processedBy")
      ).void

    def spontaneous(user: User, mod: User): Fu[Report] = ofModId(mod.id) flatMap { current =>
      current.??(cancel(mod)) >> {
        val report = Report.make(
          user, Reason.Other, Report.spontaneousText, mod
        ).copy(inquiry = Report.Inquiry(mod.id, DateTime.now).some)
        coll.insert(report) inject report
      }
    }

    private[report] def expire: Funit = {
      val selector = $doc(
        "inquiry.mod" $exists true,
        "inquiry.seenAt" $lt DateTime.now.minusMinutes(20)
      )
      coll.remove(selector ++ $doc("text" -> Report.spontaneousText)) >>
        coll.update(selector, $unset("inquiry"), multi = true).void
    }
  }
}
