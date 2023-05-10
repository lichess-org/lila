package lila.report

import com.softwaremill.macwire.*
import reactivemongo.api.ReadPreference

import lila.common.{ Bus, Heapsort }
import lila.db.dsl.{ *, given }
import lila.game.GameRepo
import lila.memo.CacheApi.*
import lila.user.{ Holder, User, UserRepo }
import lila.common.config.Max

final class ReportApi(
    val coll: Coll,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    autoAnalysis: AutoAnalysis,
    securityApi: lila.security.SecurityApi,
    userLoginsApi: lila.security.UserLoginsApi,
    playbanApi: lila.playban.PlaybanApi,
    ircApi: lila.irc.IrcApi,
    isOnline: lila.socket.IsOnline,
    cacheApi: lila.memo.CacheApi,
    snoozer: lila.memo.Snoozer[Report.SnoozeKey],
    thresholds: Thresholds,
    domain: lila.common.config.NetDomain
)(using Executor, Scheduler):

  import BSONHandlers.given
  import Report.Candidate

  private lazy val accuracyOf = accuracy.apply

  private lazy val scorer = wire[ReportScore]

  def create(data: ReportSetup, reporter: Reporter): Funit =
    Reason(data.reason) ?? { reason =>
      getSuspect(data.user.id).flatMapz: suspect =>
        create:
          Report.Candidate(
            reporter,
            suspect,
            reason,
            data.text take 1000
          )
    }

  def create(c: Candidate, score: Report.Score => Report.Score = identity): Funit =
    (!c.reporter.user.marks.reportban && !isAlreadySlain(c)) ?? {
      scorer(c) map (_ withScore score) flatMap { case scored @ Candidate.Scored(candidate, _) =>
        coll
          .one[Report](
            $doc(
              "user"   -> candidate.suspect.user.id,
              "reason" -> candidate.reason,
              "open"   -> true
            )
          )
          .flatMap { prev =>
            val report = Report.make(scored, prev)
            lila.mon.mod.report.create(report.reason.key, scored.score.value.toInt).increment()
            if (
              report.isRecentComm &&
              report.score.value >= thresholds.discord() &&
              prev.exists(_.score.value < thresholds.discord())
            ) ircApi.commReportBurst(c.suspect.user)
            coll.update.one($id(report.id), report, upsert = true).void >>
              autoAnalysis(candidate) >>- {
                if (report.isCheat)
                  Bus.publish(lila.hub.actorApi.report.CheatReportCreated(report.user), "cheatReport")
              }
          } >>-
          maxScoreCache.invalidateUnit()
      }
    }

  def commFlag(reporter: Reporter, suspect: Suspect, resource: String, text: String) =
    create(
      Candidate(
        reporter,
        suspect,
        Reason.Comm,
        s"${Reason.Comm.flagText} $resource ${text take 140}"
      )
    )

  def autoCommFlag(suspectId: SuspectId, resource: String, text: String, critical: Boolean = false) =
    getLichessReporter flatMap { reporter =>
      getSuspect(suspectId.value) flatMapz { suspect =>
        create(
          Candidate(
            reporter,
            suspect,
            Reason.Comm,
            s"${Reason.Comm.flagText} $resource ${text take 140}"
          ),
          score = (_: Report.Score).map(_ * (if critical then 2 else 1))
        )
      }
    }

  private def isAlreadySlain(candidate: Candidate) =
    (candidate.isCheat && candidate.suspect.user.marks.engine) ||
      (candidate.isAutomatic && candidate.isOther && candidate.suspect.user.marks.troll) ||
      (candidate.isComm && candidate.suspect.user.marks.troll)

  def getMod[U: UserIdOf](u: U): Fu[Option[Mod]]         = userRepo byId u dmap2 Mod.apply
  def getSuspect[U: UserIdOf](u: U): Fu[Option[Suspect]] = userRepo byId u dmap2 Suspect.apply

  def getLichessMod: Fu[Mod] = userRepo.lichess dmap2 Mod.apply orFail "User lichess is missing"
  def getLichessReporter: Fu[Reporter] =
    getLichessMod map { l =>
      Reporter(l.user)
    }

  def autoAltPrintReport(userId: UserId): Funit =
    coll.exists(
      $doc(
        "user"   -> userId,
        "reason" -> Reason.AltPrint.key
      )
    ) flatMap {
      if _ then funit // only report once
      else
        getSuspect(userId) zip getLichessReporter flatMap {
          case (Some(suspect), reporter) =>
            create(
              Candidate(
                reporter = reporter,
                suspect = suspect,
                reason = Reason.AltPrint,
                text = "Shares print with suspicious accounts"
              )
            )
          case _ => funit
        }
    }

  def autoCheatReport(userId: UserId, text: String): Funit =
    getSuspect(userId) zip
      getLichessReporter zip
      findRecent(1, selectRecent(SuspectId(userId), Reason.Cheat)).map(_.flatMap(_.atoms.toList)) flatMap {
        case ((Some(suspect), reporter), atoms) if atoms.forall(_.byHuman) =>
          lila.mon.cheat.autoReport.increment()
          create(
            Candidate(
              reporter = reporter,
              suspect = suspect,
              reason = Reason.Cheat,
              text = text
            )
          )
        case _ => funit
      }

  def autoCheatDetectedReport(userId: UserId, cheatedGames: Int): Funit =
    userRepo.byId(userId) zip getLichessReporter flatMap {
      case (Some(user), reporter) if !user.marks.engine =>
        lila.mon.cheat.autoReport.increment()
        create(
          Candidate(
            reporter = reporter,
            suspect = Suspect(user),
            reason = Reason.Cheat,
            text = s"$cheatedGames cheat detected in the last 6 months; last one is correspondence"
          )
        )
      case _ => funit
    }

  def autoBotReport(userId: UserId, referer: Option[String], name: String): Funit =
    getSuspect(userId) zip getLichessReporter flatMap {
      case (Some(suspect), reporter) =>
        create(
          Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Cheat,
            text = s"""$name bot detected on ${referer | "?"}"""
          )
        )
      case _ => funit
    }

  def maybeAutoPlaybanReport(userId: UserId, minutes: Int): Funit =
    (minutes > 60 * 24) ?? userLoginsApi.getUserIdsWithSameIpAndPrint(userId) flatMap { ids =>
      playbanApi
        .bans(userId :: ids.toList)
        .map {
          _ filter { case (_, bans) => bans > 4 }
        }
        .flatMap { bans =>
          val topSum = Heapsort.topNToList(bans.values, 10).sum
          (topSum >= 80) ?? {
            userRepo.byId(userId) zip
              getLichessReporter zip
              findRecent(1, selectRecent(SuspectId(userId), Reason.Playbans)) flatMap {
                case ((Some(abuser), reporter), past) if past.isEmpty =>
                  create(
                    Candidate(
                      reporter = reporter,
                      suspect = Suspect(abuser),
                      reason = Reason.Playbans,
                      text =
                        s"${bans.values.sum} playbans over ${bans.keys.size} accounts with IP+Print match."
                    )
                  )
                case _ => funit
              }
          }
        }
    }

  def processAndGetBySuspect(suspect: Suspect): Fu[List[Report]] =
    for {
      all <- recent(suspect, 10)
      open = all.filter(_.open)
      _ <- doProcessReport(
        $inIds(all.filter(_.open).map(_.id)),
        User.lichessId into ModId,
        unsetInquiry = false
      )
    } yield open

  def reopenReports(suspect: Suspect): Funit =
    for {
      all <- recent(suspect, 10)
      closed = all
        .filter(_.done.map(_.by) has User.lichessId.into(ModId))
        .filterNot(_ isAlreadySlain suspect.user)
      _ <-
        coll.update
          .one(
            $inIds(closed.map(_.id)),
            $set("open" -> true) ++ $unset("done"),
            multi = true
          )
          .void
    } yield ()

  // `seriousness` depends on the number of previous warnings, and number of games throwed away
  def autoBoostReport(winnerId: UserId, loserId: UserId, seriousness: Int): Funit =
    securityApi.shareAnIpOrFp(winnerId, loserId) zip
      userRepo.pair(winnerId, loserId) zip getLichessReporter flatMap {
        case ((isSame, Some((winner, loser))), reporter) if !winner.lame && !loser.lame =>
          val loginsText =
            if (isSame) "Found matching IP/print"
            else "No IP/print match found"
          create(
            Candidate(
              reporter = reporter,
              suspect = Suspect(winner),
              reason = Reason.Boost,
              text = s"Boosting: farms rating points from @${loser.username} ($loginsText)"
            ),
            _ + Report.Score(seriousness)
          )
        case _ => funit
      }

  def autoSandbagReport(winnerIds: List[UserId], loserId: UserId, seriousness: Int): Funit =
    userRepo.byId(loserId) zip getLichessReporter flatMap {
      case (Some(loser), reporter) if !loser.lame =>
        create(
          Candidate(
            reporter = reporter,
            suspect = Suspect(loser),
            reason = Reason.Boost,
            text = s"Sandbagging: throws games to ${winnerIds.map("@" + _) mkString " "}"
          ),
          _ + Report.Score(seriousness)
        )
      case _ => funit
    }

  def byId(id: Report.Id) = coll.byId[Report](id)

  def process(mod: Mod, report: Report): Funit =
    accuracy.invalidate($id(report.id)) >>
      doProcessReport($id(report.id), mod.id, unsetInquiry = true).void >>-
      maxScoreCache.invalidateUnit() >>-
      lila.mon.mod.report.close.increment().unit

  def autoProcess(mod: ModId, sus: Suspect, rooms: Set[Room]): Funit = {
    val selector = $doc(
      "user" -> sus.user.id,
      "room" $in rooms,
      "open" -> true
    )
    doProcessReport(selector, mod, unsetInquiry = true).void >>-
      maxScoreCache.invalidateUnit() >>-
      lila.mon.mod.report.close.increment().unit
  }

  private def doProcessReport(selector: Bdoc, by: ModId, unsetInquiry: Boolean): Funit =
    coll.update
      .one(
        selector,
        $set(
          "open" -> false,
          "done" -> Report.Done(by, nowInstant)
        ) ++ (unsetInquiry ?? $unset("inquiry")),
        multi = true
      )
      .void

  def autoCommReport(userId: UserId, text: String, critical: Boolean): Funit =
    getSuspect(userId) zip getLichessReporter flatMap {
      case (Some(suspect), reporter) =>
        create(
          Candidate(
            reporter = reporter,
            suspect = suspect,
            reason = Reason.Comm,
            text = text
          ),
          score = (_: Report.Score).map(_ * (if critical then 2 else 1))
        )
      case _ => funit
    }

  def moveToXfiles(id: UserId): Funit =
    coll.update
      .one(
        $id(id),
        $set("room" -> Room.Xfiles.key) ++ $unset("inquiry")
      )
      .void

  private val closedSelect: Bdoc = $doc("open" -> false)
  private val sortLastAtomAt     = $doc("atoms.0.at" -> -1)

  private def roomSelect(room: Option[Room]): Bdoc =
    room.fold($doc("room" $in Room.allButXfiles)) { r =>
      $doc("room" -> r)
    }

  private def selectOpenInRoom(room: Option[Room], exceptIds: Iterable[Report.Id]) =
    $doc("open" -> true) ++ roomSelect(room) ++ {
      exceptIds.nonEmpty ?? $doc("_id" $nin exceptIds)
    }

  private def selectOpenAvailableInRoom(room: Option[Room], exceptIds: Iterable[Report.Id]) =
    selectOpenInRoom(room, exceptIds) ++ $doc("inquiry" $exists false)

  private val maxScoreCache = cacheApi.unit[Room.Scores] {
    _.refreshAfterWrite(5 minutes)
      .buildAsyncFuture { _ =>
        Room.allButXfiles
          .map { room =>
            coll // hits the best_open partial index
              .primitiveOne[Float](
                selectOpenAvailableInRoom(room.some, Nil),
                $sort desc "score",
                "score"
              )
              .dmap(room -> _)
          }
          .parallel
          .dmap { scores =>
            Room.Scores(scores.map { (room, s) =>
              room -> s.??(_.toInt)
            }.toMap)
          }
          .addEffect { scores =>
            lila.mon.mod.report.highest.update(scores.highest).unit
          }
      }
  }

  def maxScores = maxScoreCache.getUnit

  def recent(
      suspect: Suspect,
      nb: Int,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Fu[List[Report]] =
    coll
      .find($doc("user" -> suspect.id.value))
      .sort(sortLastAtomAt)
      .cursor[Report](readPreference)
      .list(nb)

  def moreLike(report: Report, nb: Int): Fu[List[Report]] =
    coll
      .find($doc("user" -> report.user, "_id" $ne report.id))
      .sort(sortLastAtomAt)
      .cursor[Report]()
      .list(nb)

  def byAndAbout(user: User, nb: Int, mod: Holder): Fu[Report.ByAndAbout] =
    for {
      by <-
        coll
          .find($doc("atoms.by" -> user.id))
          .sort(sortLastAtomAt)
          .cursor[Report](temporarilyPrimary)
          .list(nb)
      about <- recent(Suspect(user), nb, temporarilyPrimary)
    } yield Report.ByAndAbout(by, Room.filterGranted(mod, about))

  def currentCheatScore(suspect: Suspect): Fu[Option[Report.Score]] =
    coll.primitiveOne[Report.Score](
      $doc(
        "user" -> suspect.user.id,
        "room" -> Room.Cheat.key,
        "open" -> true
      ),
      "score"
    )

  def currentCheatReport(suspect: Suspect): Fu[Option[Report]] =
    coll.one[Report](
      $doc(
        "user" -> suspect.user.id,
        "room" -> Room.Cheat.key,
        "open" -> true
      )
    )

  def recentReportersOf(sus: Suspect): Fu[List[ReporterId]] =
    coll.distinctEasy[ReporterId, List](
      "atoms.by",
      $doc(
        "user" -> sus.user.id,
        "atoms.0.at" $gt nowInstant.minusDays(3)
      ),
      ReadPreference.secondaryPreferred
    ) dmap (_ filterNot ReporterId.lichess.==)

  def openAndRecentWithFilter(mod: Mod, nb: Int, room: Option[Room]): Fu[List[Report.WithSuspect]] =
    for {
      opens <- findBest(nb, selectOpenInRoom(room, snoozedIdsOf(mod)))
      nbClosed = nb - opens.size
      closed <-
        if (room.has(Room.Xfiles) || nbClosed < 1) fuccess(Nil)
        else findRecent(nbClosed, closedSelect ++ roomSelect(room))
      withNotes <- addSuspectsAndNotes(opens ++ closed)
    } yield withNotes

  private def findNext(mod: Mod, room: Room): Fu[Option[Report]] =
    findBest(1, selectOpenAvailableInRoom(room.some, snoozedIdsOf(mod))).map(_.headOption)

  private def snoozedIdsOf(mod: Mod) = snoozer snoozedKeysOf mod.user.id map (_.reportId)

  private def addSuspectsAndNotes(reports: List[Report]): Fu[List[Report.WithSuspect]] =
    userRepo byIdsSecondary reports.map(_.user).distinct map { users =>
      reports
        .flatMap { r =>
          users.find(_.id == r.user) map { u =>
            Report.WithSuspect(r, Suspect(u), isOnline(u.id))
          }
        }
        .sortBy(-_.urgency)
    }

  def snooze(mod: Mod, reportId: Report.Id, duration: String): Fu[Option[Report]] =
    byId(reportId) flatMapz { report =>
      snoozer.set(Report.SnoozeKey(mod.user.id, reportId), duration)
      inquiries.toggleNext(mod, report.room)
    }

  object accuracy:

    private val cache =
      cacheApi[ReporterId, Option[Accuracy]](512, "report.accuracy") {
        _.expireAfterWrite(24 hours)
          .buildAsyncFuture { reporterId =>
            coll
              .find(
                $doc(
                  "atoms.by" -> reporterId,
                  "room"     -> Room.Cheat.key,
                  "open"     -> false
                )
              )
              .sort(sortLastAtomAt)
              .cursor[Report](ReadPreference.secondaryPreferred)
              .list(20) flatMap { reports =>
              if (reports.sizeIs < 4) fuccess(none) // not enough data to know
              else
                val userIds = reports.map(_.user).distinct
                userRepo countEngines userIds map { nbEngines =>
                  Accuracy {
                    Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100)
                  }.some
                }
            }
          }
      }

    private def of(reporter: ReporterId): Fu[Option[Accuracy]] =
      cache get reporter

    def apply(candidate: Candidate): Fu[Option[Accuracy]] =
      candidate.isCheat ?? of(candidate.reporter.id)

    def invalidate(selector: Bdoc): Funit =
      coll
        .distinctEasy[ReporterId, List]("atoms.by", selector, ReadPreference.secondaryPreferred)
        .map(_ foreach cache.invalidate)
        .void

  private def findRecent(nb: Int, selector: Bdoc): Fu[List[Report]] =
    (nb > 0) ?? coll.find(selector).sort(sortLastAtomAt).cursor[Report]().list(nb)

  private def findBest(nb: Int, selector: Bdoc): Fu[List[Report]] =
    (nb > 0) ?? coll.find(selector).sort($sort desc "score").cursor[Report]().list(nb)

  private def selectRecent(suspect: SuspectId, reason: Reason): Bdoc =
    $doc(
      "atoms.0.at" $gt nowInstant.minusDays(7),
      "user"   -> suspect.value,
      "reason" -> reason
    )

  object inquiries:

    private val workQueue = lila.hub.AsyncActorSequencer(
      maxSize = Max(32),
      timeout = 20 seconds,
      name = "report.inquiries"
    )

    def allBySuspect: Fu[Map[UserId, Report.Inquiry]] =
      coll.list[Report]($doc("inquiry.mod" $exists true)) map {
        _.view
          .flatMap { r =>
            r.inquiry map { i =>
              r.user -> i
            }
          }
          .toMap
      }

    def ofModId(modId: UserId): Fu[Option[Report]] = coll.one[Report]($doc("inquiry.mod" -> modId))

    def ofSuspectId(suspectId: UserId): Fu[Option[Report.Inquiry]] =
      coll.primitiveOne[Report.Inquiry]($doc("inquiry.mod" $exists true, "user" -> suspectId), "inquiry")

    def ongoingAppealOf(suspectId: UserId): Fu[Option[Report.Inquiry]] =
      coll.primitiveOne[Report.Inquiry](
        $doc(
          "inquiry.mod" $exists true,
          "user"         -> suspectId,
          "room"         -> Room.Other.key,
          "atoms.0.text" -> Report.appealText
        ),
        "inquiry"
      )

    /*
     * If the mod has no current inquiry, just start this one.
     * If they had another inquiry, cancel it and start this one instead.
     * If they already are on this inquiry, cancel it.
     * Returns the previous and next inquiries
     */
    def toggle(mod: Mod, id: String | Either[Report.Id, UserId]): Fu[(Option[Report], Option[Report])] =
      workQueue {
        doToggle(mod, id)
      }

    private def doToggle(
        mod: Mod,
        id: String | Either[Report.Id, UserId]
    ): Fu[(Option[Report], Option[Report])] =
      def findByUser(userId: UserId) = coll.one[Report]($doc("user" -> userId, "inquiry.mod" $exists true))
      for {
        report <- id match
          case Left(reportId) => coll.byId[Report](reportId)
          case Right(userId)  => findByUser(userId)
          case anyId: String  => coll.byId[Report](anyId) orElse findByUser(UserId(anyId))
        current <- ofModId(mod.user.id)
        _       <- current ?? cancel(mod)
        _ <-
          report ?? { r =>
            r.inquiry.isEmpty ?? coll
              .updateField(
                $id(r.id),
                "inquiry",
                Report.Inquiry(mod.user.id, nowInstant)
              )
              .void
          }
      } yield (current, report.filter(_.inquiry.isEmpty))

    def toggleNext(mod: Mod, room: Room): Fu[Option[Report]] =
      workQueue {
        findNext(mod, room) flatMapz { report =>
          doToggle(mod, Left(report.id)).dmap(_._2)
        }
      }

    private def cancel(mod: Mod)(report: Report): Funit =
      if (report.isOther && report.onlyAtom.map(_.by.value).has(mod.user.id))
        coll.delete.one($id(report.id)).void // cancel spontaneous inquiry
      else
        coll.update
          .one(
            $id(report.id),
            $unset("inquiry", "done") ++ $set("open" -> true)
          )
          .void

    def spontaneous(mod: Mod, sus: Suspect): Fu[Report] =
      openOther(mod, sus, Report.spontaneousText)

    def appeal(mod: Mod, sus: Suspect): Fu[Report] =
      openOther(mod, sus, Report.appealText)

    private def openOther(mod: Mod, sus: Suspect, name: String): Fu[Report] =
      ofModId(mod.user.id) flatMap { current =>
        current.??(cancel(mod)) >> {
          val report = Report
            .make(
              Candidate(
                Reporter(mod.user),
                sus,
                Reason.Other,
                name
              ) scored Report.Score(0),
              none
            )
            .copy(inquiry = Report.Inquiry(mod.user.id, nowInstant).some)
          coll.insert.one(report) inject report
        }
      }

    private[report] def expire: Funit =
      workQueue {
        val selector = $doc(
          "inquiry.mod" $exists true,
          "inquiry.seenAt" $lt nowInstant.minusMinutes(20)
        )
        coll.delete.one(selector ++ $doc("text" -> Report.spontaneousText)) >>
          coll.update.one(selector, $unset("inquiry"), multi = true).void
      }
