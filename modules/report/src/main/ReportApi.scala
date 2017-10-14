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
    autoAnalysis: AutoAnalysis,
    noteApi: NoteApi,
    securityApi: lila.security.SecurityApi,
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

      def insert = coll.insert(report).void >>
        autoAnalysis(report) >>-
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

  def getMod(username: String): Fu[Option[Mod]] =
    UserRepo named username map2 Mod.apply

  def getLichess: Fu[Option[Mod]] = UserRepo.lichess map2 Mod.apply

  def getSuspect(username: String): Fu[Option[Suspect]] =
    UserRepo named username map2 Suspect.apply

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

  def autoBoostReport(winnerId: User.ID, loserId: User.ID): Funit =
    securityApi.shareIpOrPrint(winnerId, loserId) zip
      UserRepo.byId(winnerId) zip UserRepo.byId(loserId) zip UserRepo.lichess flatMap {
        case isSame ~ Some(winner) ~ Some(loser) ~ Some(lichess) => create(ReportSetup(
          user = if (isSame) winner else loser,
          reason = Reason.Boost.key,
          text =
            if (isSame) s"Farms rating points from @${loser.username} (same IP or print)"
            else s"Sandbagging - the winning player @${winner.username} has different IPs & prints",
          gameId = "",
          move = ""
        ), lichess)
        case _ => funit
      }

  private def publishProcessed(sus: Suspect, reason: Reason) =
    bus.publish(lila.hub.actorApi.report.Processed(sus.user.id, reason.key), 'report)

  def process(mod: Mod, reportId: Report.ID): Funit = for {
    report <- coll.byId[Report](reportId) flatten s"no such report $reportId"
    suspect <- getSuspect(report.user) flatten s"No such suspect $report"
    rooms = Set(Room(report.reason))
    res <- process(mod, suspect, rooms, reportId.some)
  } yield res

  def process(mod: Mod, sus: Suspect, rooms: Set[Room], reportId: Option[Report.ID] = None): Funit =
    inquiries.ofModId(mod.user.id) map (_.filter(_.user == sus.user.id)) flatMap { inquiry =>
      val relatedSelector = $doc(
        "user" -> sus.user.id,
        "room" $in rooms,
        "processedBy" $exists false
      )
      val reportSelector = reportId.orElse(inquiry.map(_.id)).fold(relatedSelector) { id =>
        $or($id(id), relatedSelector)
      }
      coll.update(
        reportSelector,
        $set("processedBy" -> mod.user.id) ++ $unset("inquiry"),
        multi = true
      ).void >> accuracy.invalidate(reportSelector) >>- {
          monitorUnprocessed
          lila.mon.mod.report.close()
          rooms.flatMap(Room.toReasons) foreach { publishProcessed(sus, _) }
        }
    }

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

  def recentReportersOf(sus: Suspect): Fu[List[User.ID]] =
    coll.distinctWithReadPreference[String, List]("createdBy", $doc(
      "user" -> sus.user.id,
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

  def next(room: Room): Fu[Option[Report]] =
    unprocessedAndRecentWithFilter(1, room.some) map (_.headOption.map(_.report))

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

  object accuracy {

    private val cache = asyncCache.clearable[User.ID, Option[Int]](
      name = "reporterAccuracy",
      f = forUser,
      expireAfter = _.ExpireAfterWrite(24 hours)
    )

    private def forUser(reporterId: User.ID): Fu[Option[Int]] =
      coll.find($doc(
        "createdBy" -> reporterId,
        "reason" -> Reason.Cheat.key,
        "processedBy" $exists true
      )).sort($sort.createdDesc).list[Report](20, ReadPreference.secondaryPreferred) flatMap { reports =>
        if (reports.size < 5) fuccess(none) // not enough data to know
        else {
          val userIds = reports.map(_.user).distinct
          UserRepo countEngines userIds map { nbEngines =>
            Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100).some
          }
        }
      }

    def apply(report: Report): Fu[Option[Int]] =
      (report.reason == Reason.Cheat && !report.processed) ?? {
        cache get report.createdBy
      }

    def invalidate(selector: Bdoc): Funit =
      coll.distinct[User.ID, List]("createdBy", selector.some).map {
        _ foreach cache.invalidate
      }.void
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

    /*
     * If the mod has no current inquiry, just start this one.
     * If they had another inquiry, cancel it and start this one instead.
     * If they already are on this inquiry, cancel it.
     */
    def toggle(mod: Mod, id: Report.ID): Fu[Option[Report]] = for {
      report <- coll.byId[Report](id) flatten s"No report $id found"
      current <- ofModId(mod.user.id)
      _ <- current ?? cancel(mod)
      isSame = current.exists(_.id == report.id)
      _ <- !isSame ?? coll.updateField(
        $id(report.id),
        "inquiry",
        Report.Inquiry(mod.user.id, DateTime.now)
      ).void
    } yield !isSame option report

    def cancel(mod: Mod)(report: Report): Funit =
      if (report.isOther && report.createdBy == mod.user.id) coll.remove($id(report.id)).void
      else coll.update(
        $id(report.id),
        $unset("inquiry", "processedBy")
      ).void

    def spontaneous(mod: Mod, sus: Suspect): Fu[Report] = ofModId(mod.user.id) flatMap { current =>
      current.??(cancel(mod)) >> {
        val report = Report.make(
          sus.user, Reason.Other, Report.spontaneousText, mod.user
        ).copy(inquiry = Report.Inquiry(mod.user.id, DateTime.now).some)
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
