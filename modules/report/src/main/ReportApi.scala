package lidraughts.report

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo, NoteApi }

final class ReportApi(
    val coll: Coll,
    autoAnalysis: AutoAnalysis,
    noteApi: NoteApi,
    securityApi: lidraughts.security.SecurityApi,
    userSpyApi: lidraughts.security.UserSpyApi,
    playbanApi: lidraughts.playban.PlaybanApi,
    isOnline: User.ID => Boolean,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    bus: lidraughts.common.Bus,
    scoreThreshold: () => Int
) {

  import BSONHandlers._

  private lazy val scorer = new ReportScore(getAccuracy = accuracy.of)

  def create(c: Report.Candidate): Funit = !c.reporter.user.reportban ?? {
    !isAlreadySlain(c) ?? {
      scorer(c) flatMap {
        case scored @ Report.Candidate.Scored(candidate, _) =>
          coll.find($doc(
            "user" -> candidate.suspect.user.id,
            "reason" -> candidate.reason,
            "open" -> true
          )).one[Report].flatMap { existing =>
            val report = Report.make(scored, existing)
            lidraughts.mon.mod.report.create(report.reason.key)()
            coll.update($id(report.id), report, upsert = true).void >>
              autoAnalysis(candidate) >>-
              bus.publish(lidraughts.hub.actorApi.report.Created(candidate.suspect.user.id, candidate.reason.key, candidate.reporter.user.id), 'report)
          } >>- monitorOpen
      }
    }
  }

  def commFlag(reporter: Reporter, suspect: Suspect, resource: String, text: String) = create(Report.Candidate(
    reporter,
    suspect,
    Reason.CommFlag,
    s"[FLAG] $resource ${text take 140}"
  ))

  private def monitorOpen = {
    nbOpenCache.refresh
    nbOpen foreach { nb =>
      lidraughts.mon.mod.report.unprocessed(nb)
    }
  }

  private def isAlreadySlain(candidate: Report.Candidate) =
    (candidate.isCheat && candidate.suspect.user.engine) ||
      (candidate.isAutomatic && candidate.isOther && candidate.suspect.user.troll) ||
      (candidate.isAboutComm && candidate.suspect.user.troll)

  def getMod(username: String): Fu[Option[Mod]] =
    UserRepo named username map2 Mod.apply

  def getLidraughtsMod: Fu[Mod] = UserRepo.Lidraughts map2 Mod.apply flatten "User lidraughts is missing"
  def getLidraughtsReporter: Fu[Reporter] = getLidraughtsMod map { l => Reporter(l.user) }

  def getSuspect(username: String): Fu[Option[Suspect]] =
    UserRepo named username map2 Suspect.apply

  def autoCheatPrintReport(userId: String): Funit =
    coll.exists($doc(
      "user" -> userId,
      "reason" -> Reason.CheatPrint.key
    )) flatMap {
      case true => funit // only report once
      case _ => getSuspect(userId) zip getLidraughtsReporter flatMap {
        case (Some(suspect), reporter) => create(Report.Candidate(
          reporter = reporter,
          suspect = suspect,
          reason = Reason.CheatPrint,
          text = "Shares print with known cheaters"
        ))
        case _ => funit
      }
    }

  def autoCheatReport(userId: String, text: String): Funit =
    getSuspect(userId) zip
      getLidraughtsReporter zip
      findRecent(1, selectRecent(SuspectId(userId), Reason.Cheat)).map(_.flatMap(_.atoms.toList)) flatMap {
        case Some(suspect) ~ reporter ~ atoms if atoms.forall(_.byHuman) =>
          lidraughts.mon.cheat.autoReport.count()
          create(Report.Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Cheat,
            text = text
          ))
        case _ => funit
      }

  def autoBotReport(userId: String, referer: Option[String], name: String): Funit =
    getSuspect(userId) zip getLidraughtsReporter flatMap {
      case (Some(suspect), reporter) => create(Report.Candidate(
        reporter = reporter,
        suspect = suspect,
        reason = Reason.Cheat,
        text = s"""$name bot detected on ${referer | "?"}"""
      ))
      case _ => funit
    }

  def maybeAutoPlaybanReport(userId: String): Funit =
    userSpyApi.getUserIdsWithSameIpAndPrint(userId) map { ids =>
      playbanApi.bans(ids.toList ::: List(userId)) map { bans =>
        (bans.values.sum >= 80) ?? {
          UserRepo.byId(userId) zip
            getLidraughtsReporter zip
            findRecent(1, selectRecent(SuspectId(userId), Reason.Playbans)) flatMap {
              case Some(abuser) ~ reporter ~ past if past.size < 1 => create(Report.Candidate(
                reporter = reporter,
                suspect = Suspect(abuser),
                reason = Reason.Playbans,
                text = s"${bans.values.sum} playbans over ${bans.keys.size} accounts with IP+Print match."
              ))
              case _ => funit
            }
        }
      }
    }

  def processAndGetBySuspect(suspect: Suspect): Fu[List[Report]] = for {
    all <- recent(suspect, 10)
    open = all.filter(_.open)
    _ <- doProcessReport($inIds(open.map(_.id)), ModId.Lidraughts)
  } yield open

  def autoBoostReport(winnerId: User.ID, loserId: User.ID): Funit =
    securityApi.shareIpOrPrint(winnerId, loserId) zip
      UserRepo.byId(winnerId) zip UserRepo.byId(loserId) zip getLidraughtsReporter flatMap {
        case isSame ~ Some(winner) ~ Some(loser) ~ reporter => create(Report.Candidate(
          reporter = reporter,
          suspect = Suspect(if (isSame) winner else loser),
          reason = Reason.Boost,
          text =
            if (isSame) s"Farms rating points from @${loser.username} (same IP or print)"
            else s"Sandbagging - the winning player @${winner.username} has different IPs & prints"
        ))
        case _ => funit
      }

  private def publishProcessed(sus: Suspect, reason: Reason) =
    bus.publish(lidraughts.hub.actorApi.report.Processed(sus.user.id, reason.key), 'report)

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
        "open" -> true
      )
      val reportSelector = reportId.orElse(inquiry.map(_.id)).fold(relatedSelector) { id =>
        $or($id(id), relatedSelector)
      }
      accuracy.invalidate(reportSelector) >>
        doProcessReport(reportSelector, mod.id).void >>- {
          monitorOpen
          lidraughts.mon.mod.report.close()
          rooms.flatMap(Room.toReasons) foreach { publishProcessed(sus, _) }
        }
    }

  private def doProcessReport(selector: Bdoc, by: ModId) = coll.update(
    selector,
    $set(
      "open" -> false,
      "processedBy" -> by.value
    ) ++ $unset("inquiry"),
    multi = true
  )

  def autoInsultReport(userId: String, text: String): Funit = {
    getSuspect(userId) zip getLidraughtsReporter flatMap {
      case (Some(suspect), reporter) => create(Report.Candidate(
        reporter = reporter,
        suspect = suspect,
        reason = Reason.Insult,
        text = text
      ))
      case _ => funit
    }
  } >>- monitorOpen

  def moveToXfiles(id: String): Funit = coll.update(
    $id(id),
    $set("room" -> Room.Xfiles.key) ++ $unset("inquiry")
  ).void

  private val openSelect: Bdoc = $doc("open" -> true)
  private val closedSelect: Bdoc = $doc("open" -> false)
  private val openAvailableSelect: Bdoc = openSelect ++ $doc("inquiry" $exists false)
  private def scoreThresholdSelect = $doc("score" $gte scoreThreshold())
  private val sortLastAtomAt = $doc("atoms.0.at" -> -1)

  private def roomSelect(room: Option[Room]): Bdoc =
    room.fold($doc("room" $ne Room.Xfiles.key)) { r => $doc("room" -> r) }

  val nbOpenCache = asyncCache.single[Int](
    name = "report.nbOpen",
    f = coll.countSel(openAvailableSelect ++ roomSelect(none) ++ scoreThresholdSelect),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )

  def nbOpen = nbOpenCache.get

  def recent(suspect: Suspect, nb: Int, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Report]] =
    coll.find($doc("user" -> suspect.id.value)).sort(sortLastAtomAt).list[Report](nb, readPreference)

  def moreLike(report: Report, nb: Int): Fu[List[Report]] =
    coll.find($doc("user" -> report.user, "_id" $ne report.id)).sort(sortLastAtomAt).list[Report](nb)

  def byAndAbout(user: User, nb: Int): Fu[Report.ByAndAbout] = for {
    by <- coll.find(
      $doc("atoms.by" -> user.id)
    ).sort(sortLastAtomAt).list[Report](nb, ReadPreference.secondaryPreferred)
    about <- recent(Suspect(user), nb, ReadPreference.secondaryPreferred)
  } yield Report.ByAndAbout(by, about)

  def currentCheatScore(suspect: Suspect): Fu[Option[Report.Score]] =
    coll.primitiveOne[Report.Score]($doc(
      "user" -> suspect.user.id,
      "room" -> Room.Cheat.key,
      "open" -> true
    ), "score")

  def currentCheatReport(suspect: Suspect): Fu[Option[Report]] =
    coll.uno[Report]($doc(
      "user" -> suspect.user.id,
      "room" -> Room.Cheat.key,
      "open" -> true
    ))

  def recentReportersOf(sus: Suspect): Fu[List[ReporterId]] =
    coll.distinctWithReadPreference[ReporterId, List](
      "atoms.by",
      $doc(
        "user" -> sus.user.id,
        "atoms.0.at" $gt DateTime.now.minusDays(3)
      ).some,
      ReadPreference.secondaryPreferred
    ) map (_ filterNot ReporterId.Lidraughts.==)

  def openAndRecentWithFilter(nb: Int, room: Option[Room]): Fu[List[Report.WithSuspect]] = for {
    opens <- findBest(nb, openAvailableSelect ++ roomSelect(room) ++ scoreThresholdSelect)
    nbClosed = nb - opens.size
    closed <- if (room.has(Room.Xfiles) || nbClosed < 1) fuccess(Nil)
    else findRecent(nbClosed, closedSelect ++ roomSelect(room))
    withNotes <- addSuspectsAndNotes(opens ++ closed)
  } yield withNotes

  def next(room: Room): Fu[Option[Report]] =
    findBest(1, openAvailableSelect ++ roomSelect(room.some) ++ scoreThresholdSelect).map(_.headOption)

  private def addSuspectsAndNotes(reports: List[Report]): Fu[List[Report.WithSuspect]] =
    UserRepo byIdsSecondary (reports.map(_.user).distinct) map { users =>
      reports.flatMap { r =>
        users.find(_.id == r.user) map { u =>
          Report.WithSuspect(r, Suspect(u), isOnline(u.id))
        }
      }.sortBy(-_.urgency)
    }

  private[report] def resetScores: Funit = scorer reset coll void

  object accuracy {

    private val cache = asyncCache.clearable[User.ID, Option[Int]](
      name = "reporterAccuracy",
      f = a => forUser(a).map2((a: Accuracy) => a.value),
      expireAfter = _.ExpireAfterWrite(24 hours)
    )

    private def forUser(reporterId: User.ID): Fu[Option[Accuracy]] =
      coll.find($doc(
        "atoms.by" -> reporterId,
        "room" -> Room.Cheat.key,
        "open" -> false
      )).sort(sortLastAtomAt).list[Report](20, ReadPreference.secondaryPreferred) flatMap { reports =>
        if (reports.size < 4) fuccess(none) // not enough data to know
        else {
          val userIds = reports.map(_.user).distinct
          UserRepo countEngines userIds map { nbEngines =>
            Accuracy {
              Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100)
            }.some
          }
        }
      }

    def of(reporter: ReporterId): Fu[Option[Accuracy]] =
      cache get reporter.value map2 Accuracy.apply

    def apply(candidate: Report.Candidate): Fu[Option[Accuracy]] =
      (candidate.reason == Reason.Cheat) ?? of(candidate.reporter.id)

    def invalidate(selector: Bdoc): Funit =
      coll.distinct[User.ID, List]("atoms.by", selector.some).map {
        _ foreach cache.invalidate
      }.void
  }

  def countOpenByRooms: Fu[Room.Counts] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match(openAvailableSelect ++ scoreThresholdSelect ++ roomSelect(none)),
      List(
        GroupField("room")("nb" -> SumValue(1))
      ),
      maxDocs = 100
    ).map { docs =>
        Room.Counts(docs.flatMap { doc =>
          doc.getAs[String]("_id") flatMap Room.apply flatMap { room =>
            doc.getAs[Int]("nb") map { room -> _ }
          }
        }.toMap)
      }
  }

  def currentlyReportedForCheat: Fu[Set[User.ID]] =
    coll.distinctWithReadPreference[User.ID, Set](
      "user",
      Some($doc("reason" -> Reason.Cheat.key) ++ openSelect),
      ReadPreference.secondaryPreferred
    )

  private def findRecent(nb: Int, selector: Bdoc): Fu[List[Report]] = (nb > 0) ?? {
    coll.find(selector).sort(sortLastAtomAt).list[Report](nb)
  }

  private def findBest(nb: Int, selector: Bdoc): Fu[List[Report]] = (nb > 0) ?? {
    coll.find(selector).sort($sort desc "score").list[Report](nb)
  }

  private def selectRecent(suspect: SuspectId, reason: Reason): Bdoc = $doc(
    "atoms.0.at" $gt DateTime.now.minusDays(7),
    "user" -> suspect.value,
    "reason" -> reason
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
      if (report.isOther && report.onlyAtom.map(_.by.value).has(mod.user.id))
        coll.remove($id(report.id)).void // cancel spontaneous inquiry
      else coll.update(
        $id(report.id),
        $unset("inquiry", "processedBy") ++ $set("open" -> true)
      ).void

    def spontaneous(mod: Mod, sus: Suspect): Fu[Report] = ofModId(mod.user.id) flatMap { current =>
      current.??(cancel(mod)) >> {
        val report = Report.make(
          Report.Candidate(
            Reporter(mod.user),
            sus,
            Reason.Other,
            Report.spontaneousText
          ) scored Report.Score(0),
          none
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
