package lila.report

import com.softwaremill.macwire.*

import lila.common.Bus
import lila.core.id.ReportId
import lila.core.report.SuspectId
import lila.core.userId.ModId
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.report.Room.Scores

final class ReportApi(
    val coll: Coll,
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    autoAnalysis: AutoAnalysis,
    securityApi: lila.core.security.SecurityApi,
    playbansOf: () => lila.core.playban.BansOf,
    ircApi: lila.core.irc.IrcApi,
    isOnline: lila.core.socket.IsOnline,
    cacheApi: lila.memo.CacheApi,
    snoozer: lila.memo.Snoozer[Report.SnoozeKey],
    thresholds: Thresholds
)(using Executor, Scheduler, lila.core.config.NetDomain)
    extends lila.core.report.ReportApi:

  import BSONHandlers.given
  import Report.Candidate

  private lazy val accuracyOf = accuracy.apply

  private lazy val scorer = wire[ReportScore]

  def create(data: ReportSetup, reporter: Reporter, msgs: List[String]): Funit =
    Reason(data.reason).so: reason =>
      getSuspect(data.user.id).flatMapz: suspect =>
        if data.text.startsWith(Reason.flagText) then
          logger.warn(s"False flag from ${reporter.user.username} about ${data.user.name}: ${data.text}")
          funit
        else
          create:
            Report.Candidate(
              reporter,
              suspect,
              reason,
              data.text.take(1000) + msgs.nonEmpty.so:
                s"""\n\n\n--- selected inbox messages ---\n\n${msgs.mkString("\n\n")}"""
            )

  def isAutoBlock(data: ReportSetup): Boolean =
    Reason(data.reason).exists(Reason.autoBlock)

  def create(c: Candidate, score: Report.Score => Report.Score = identity): Funit =
    (!c.reporter.user.marks.reportban && !isAlreadySlain(c)).so:
      scorer(c).map(_.withScore(score)).flatMap { case scored @ Candidate.Scored(candidate, _) =>
        for
          prev <- coll.one[Report]:
            $doc(
              "user" -> candidate.suspect.user.id,
              "room" -> Room(candidate.reason),
              "open" -> true
            )
          report = Report.make(scored, prev)
          _ = lila.mon.mod.report.create(report.room.key, scored.score.value.toInt).increment()
          _ = if report.isRecentComm &&
            report.score.value >= thresholds.discord() &&
            prev.exists(_.score.value < thresholds.discord())
          then ircApi.commReportBurst(c.suspect.user.light)
          _ <- coll.update.one($id(report.id), report, upsert = true)
          _ <- autoAnalysis(candidate)
        yield
          if report.is(_.Cheat) then Bus.pub(lila.core.report.CheatReportCreated(report.user))
          maxScoreCache.invalidateUnit()
      }

  def commFlag(reporter: Reporter, suspect: Suspect, resource: String, text: String) = create:
    Candidate(
      reporter,
      suspect,
      Reason.Comm,
      s"${Reason.flagText} $resource ${text.take(140)}"
    )

  def autoCommFlag(suspectId: SuspectId, resource: String, text: String, critical: Boolean = false) =
    getLichessReporter.flatMap { reporter =>
      getSuspect(suspectId.value).flatMapz { suspect =>
        create(
          Candidate(
            reporter,
            suspect,
            Reason.Comm,
            s"${Reason.flagText} $resource ${text.take(140)}"
          ),
          score = (_: Report.Score).map(_ * (if critical then 2 else 1))
        )
      }
    }

  private def isAlreadySlain(candidate: Candidate) =
    (candidate.isCheat && candidate.suspect.user.marks.engine) ||
      (candidate.isAutomatic && candidate.reason == Reason.Other && candidate.suspect.user.marks.troll)

  def getMyMod(using me: MyId): Fu[Option[Mod]] = userApi.byId(me).dmap2(Mod.apply)
  def getMod[U: UserIdOf](u: U): Fu[Option[Mod]] = userApi.byId(u).dmap2(Mod.apply)
  def getSuspect[U: UserIdOf](u: U): Fu[Option[Suspect]] = userApi.byId(u).dmap2(Suspect.apply)

  def getLichessMod: Fu[Mod] = userApi.byId(UserId.lichess).dmap2(Mod.apply).orFail("User lichess is missing")
  def getLichessReporter: Fu[Reporter] =
    getLichessMod.map: l =>
      Reporter(l.user)

  def autoAltPrintReport(userId: UserId): Funit =
    coll
      .exists(
        $doc(
          "user" -> userId,
          "room" -> Room(Reason.AltPrint).key
        )
      )
      .flatMap:
        if _ then funit // only report once
        else
          getSuspect(userId)
            .zip(getLichessReporter)
            .flatMap:
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

  def autoCheatReport(userId: UserId, text: String): Funit =
    getSuspect(userId)
      .zip(getLichessReporter)
      .zip(findRecent(1, selectRecent(SuspectId(userId), Reason.Cheat)).map(_.flatMap(_.atoms.toList)))
      .flatMap:
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

  def autoCheatDetectedReport(userId: UserId, cheatedGames: Int): Funit =
    userApi
      .byId(userId)
      .zip(getLichessReporter)
      .flatMap:
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

  def autoBotReport(userId: UserId, referer: Option[String], name: String): Funit =
    getSuspect(userId)
      .zip(getLichessReporter)
      .flatMap:
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

  def maybeAutoPlaybanReport(userId: UserId, minutes: Int): Funit =
    (minutes > 60 * 24).so(securityApi.getUserIdsWithSameIpAndPrint(userId)).flatMap { ids =>
      playbansOf()(userId :: ids.toList)
        .map:
          _.filter { (_, bans) => bans > 4 }
        .flatMap: bans =>
          val topSum = scalalib.HeapSort.topNToList(bans.values, 10).sum
          (topSum >= 80).so:
            userApi
              .byId(userId)
              .zip(getLichessReporter)
              .zip(findRecent(1, selectRecent(SuspectId(userId), Reason.Playbans)))
              .flatMap:
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

  def processAndGetBySuspect(suspect: Suspect): Fu[List[Report]] =
    for
      all <- recent(suspect, Max(10))
      open = all.filter(_.open)
      _ <- doProcessReport(
        $inIds(all.filter(_.open).map(_.id)),
        unsetInquiry = false
      )(using UserId.lichessAsMe)
    yield open

  def reopenReports(suspect: Suspect): Funit =
    for
      all <- recent(suspect, Max(10))
      closed = all
        .filter(_.done.map(_.by).has(UserId.lichess.into(ModId)))
        .filterNot(_.isAlreadySlain(suspect.user))
      _ <-
        coll.update
          .one(
            $inIds(closed.map(_.id)),
            $set("open" -> true) ++ $unset("done"),
            multi = true
          )
          .void
    yield ()

  // `seriousness` depends on the number of previous warnings, and number of games thrown away
  def autoBoostReport(winnerId: UserId, loserId: UserId, seriousness: Int): Funit =
    securityApi
      .shareAnIpOrFp(winnerId, loserId)
      .zip(userApi.pair(winnerId, loserId))
      .zip(getLichessReporter)
      .flatMap:
        case ((isSame, Some((winner, loser))), reporter) if !winner.lame && !loser.lame =>
          val loginsText =
            if isSame then "Found matching IP/print"
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

  def autoSandbagReport(winnerIds: List[UserId], loserId: UserId, seriousness: Int): Funit =
    userApi
      .byId(loserId)
      .zip(getLichessReporter)
      .flatMap:
        case (Some(loser), reporter) if !loser.lame =>
          create(
            Candidate(
              reporter = reporter,
              suspect = Suspect(loser),
              reason = Reason.Boost,
              text = s"Sandbagging: throws games to ${winnerIds.map("@" + _).mkString(" ")}"
            ),
            _ + Report.Score(seriousness)
          )
        case _ => funit

  def byId(id: ReportId) = coll.byId[Report](id)

  def process(report: Report)(using Me): Funit = for
    _ <- accuracy.invalidate($id(report.id))
    deletedAppeal <- deleteIfAppealInquiry(report)
    _ <- (!deletedAppeal).so:
      doProcessReport($id(report.id), unsetInquiry = true)
  yield onReportClose()

  def autoProcess(sus: Suspect, rooms: Set[Room])(using MyId): Funit =
    val selector = $doc(
      "user" -> sus.user.id,
      "room".$in(rooms),
      "open" -> true
    )
    for _ <- doProcessReport(selector, unsetInquiry = true)
    yield onReportClose()

  private def onReportClose() =
    maxScoreCache.invalidateUnit()
    lila.mon.mod.report.close.increment()

  private def deleteIfAppealInquiry(report: Report)(using me: MyId): Fu[Boolean] =
    if report.isAppealInquiryByMe
    then for _ <- coll.delete.one($id(report.id)) yield true
    else fuccess(false)

  private def doProcessReport(selector: Bdoc, unsetInquiry: Boolean)(using me: MyId): Funit =
    coll.update
      .one(
        selector,
        $set(
          "open" -> false,
          "done" -> Report.Done(me.modId, nowInstant)
        ) ++ (unsetInquiry.so($unset("inquiry"))),
        multi = true
      )
      .void

  def autoCommReport(userId: UserId, text: String, critical: Boolean): Funit =
    getSuspect(userId)
      .zip(getLichessReporter)
      .flatMap:
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

  def moveToXfiles(id: ReportId): Funit =
    coll.update
      .one(
        $id(id),
        $set("room" -> Room.Xfiles.key) ++ $unset("inquiry")
      )
      .void

  private val closedSelect: Bdoc = $doc("open" -> false)
  private val sortLastAtomAt = $doc("atoms.0.at" -> -1)

  private def roomSelect(room: Option[Room]): Bdoc =
    room.fold($doc("room".$in(Room.allButXfiles))): r =>
      $doc("room" -> r)

  private def selectOpenInRoom(room: Option[Room], exceptIds: Iterable[ReportId]) =
    $doc("open" -> true) ++ roomSelect(room) ++ {
      exceptIds.nonEmpty.so($doc("_id".$nin(exceptIds)))
    }

  private def selectOpenAvailableInRoom(room: Option[Room], exceptIds: Iterable[ReportId]) =
    selectOpenInRoom(room, exceptIds) ++ $doc("inquiry".$exists(false))

  private val maxScoreCache = cacheApi.unit[Room.Scores]:
    _.refreshAfterWrite(5.minutes).buildAsyncFuture: _ =>
      Room.allButXfiles
        .parallel: room =>
          coll // hits the best_open partial index
            .primitiveOne[Float](
              selectOpenAvailableInRoom(room.some, Nil),
              $sort.desc("score"),
              "score"
            )
            .dmap(room -> _)
        .dmap: scores =>
          Room.Scores:
            scores
              .map: (room, s) =>
                room -> s.so(_.toInt)
              .toMap
        .addEffect: scores =>
          lila.mon.mod.report.highest.update(scores.highest)

  def maxScores: Fu[Scores] = maxScoreCache.getUnit

  def recent(
      suspect: Suspect,
      nb: Max,
      readPref: ReadPref = _.sec
  ): Fu[List[Report]] =
    coll
      .find($doc("user" -> suspect.id.value))
      .sort(sortLastAtomAt)
      .cursor[Report](readPref)
      .list(nb.value)

  def moreLike(report: Report, nb: Max): Fu[List[Report]] =
    coll
      .find($doc("user" -> report.user, "_id".$ne(report.id)))
      .sort(sortLastAtomAt)
      .cursor[Report]()
      .list(nb.value)

  def allReportsAbout(user: User, nb: Max, select: Bdoc = $empty): Fu[List[Report]] =
    coll
      .find($doc("user" -> user.id) ++ select)
      .sort(sortLastAtomAt)
      .cursor[Report]()
      .list(nb.value)

  def commReportsAbout(user: User, nb: Max): Fu[List[Report]] =
    allReportsAbout(user, nb, $doc("room" -> Room.Comm.key))

  def by(user: User, nb: Max): Fu[List[Report]] =
    coll
      .find($doc("atoms.by" -> user.id))
      .sort(sortLastAtomAt)
      .cursor[Report](ReadPref.sec)
      .list(nb.value)

  def personalExport(user: User): Fu[List[Report.Atom]] =
    coll
      .list[Report]($doc("atoms.by" -> user.id))
      .map:
        _.flatMap(_.atomBy(user.id.into(ReporterId)))

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
    coll.one[Report]:
      $doc(
        "user" -> suspect.user.id,
        "room" -> Room.Cheat.key,
        "open" -> true
      )

  def recentReportersOf(sus: Suspect): Fu[List[ReporterId]] =
    coll
      .distinctEasy[ReporterId, List](
        "atoms.by",
        $doc(
          "user" -> sus.user.id,
          "atoms.0.at".$gt(nowInstant.minusDays(3))
        ),
        _.sec
      )
      .dmap(_.filterNot(ReporterId.lichess.==))

  def openAndRecentWithFilter(nb: Int, room: Option[Room])(using mod: Me): Fu[List[Report.WithSuspect]] =
    for
      opens <- findBest(nb, selectOpenInRoom(room, snoozedIds))
      nbClosed = nb - opens.size
      closed <-
        if room.has(Room.Xfiles) || nbClosed < 1 then fuccess(Nil)
        else findRecent(nbClosed, closedSelect ++ roomSelect(room))
      withNotes <- addSuspectsAndNotes(opens ++ closed)
    yield withNotes

  private def findNext(room: Room)(using Me): Fu[Option[Report]] =
    findBest(1, selectOpenAvailableInRoom(room.some, snoozedIds)).map(_.headOption)

  private def snoozedIds(using mod: Me) = snoozer.snoozedKeysOf(mod.userId).map(_.reportId)

  private def addSuspectsAndNotes(reports: List[Report]): Fu[List[Report.WithSuspect]] =
    userApi
      .listWithPerfs(reports.map(_.user).distinct)
      .map: users =>
        reports
          .flatMap: r =>
            users.find(_.id == r.user).map { u => Report.WithSuspect(r, u, isOnline.exec(u.id)) }
          .sortBy(-_.urgency)

  def snooze(reportId: ReportId, duration: String)(using mod: Me): Fu[Option[Report]] =
    byId(reportId).flatMapz { report =>
      snoozer.set(Report.SnoozeKey(mod.userId, reportId), duration)
      inquiries.toggleNext(report.room)
    }

  object accuracy:

    private val cache =
      cacheApi[ReporterId, Option[Accuracy]](512, "report.accuracy"):
        _.expireAfterWrite(24.hours).buildAsyncFuture: reporterId =>
          coll
            .find:
              $doc(
                "atoms.by" -> reporterId,
                "room" -> Room.Cheat.key,
                "open" -> false
              )
            .sort(sortLastAtomAt)
            .cursor[Report](ReadPref.sec)
            .list(20)
            .flatMap: reports =>
              if reports.sizeIs < 4 then fuccess(none) // not enough data to know
              else
                val userIds = reports.map(_.user).distinct
                userApi.countEngines(userIds).map { nbEngines =>
                  Accuracy {
                    Math.round((nbEngines + 0.5f) / (userIds.length + 2f) * 100)
                  }.some
                }

    private def of(reporter: ReporterId): Fu[Option[Accuracy]] =
      cache.get(reporter)

    def apply(candidate: Candidate): Fu[Option[Accuracy]] =
      candidate.isCheat.so(of(candidate.reporter.id))

    def invalidate(selector: Bdoc): Funit =
      coll
        .distinctEasy[ReporterId, List]("atoms.by", selector, _.sec)
        .map(_.foreach(cache.invalidate))
        .void

  private def findRecent(nb: Int, selector: Bdoc): Fu[List[Report]] =
    (nb > 0).so(coll.find(selector).sort(sortLastAtomAt).cursor[Report]().list(nb))

  private def findBest(nb: Int, selector: Bdoc): Fu[List[Report]] =
    (nb > 0).so(coll.find(selector).sort($sort.desc("score")).cursor[Report]().list(nb))

  private def selectRecent(suspect: SuspectId, reason: Reason): Bdoc =
    $doc(
      "atoms.0.at".$gt(nowInstant.minusDays(7)),
      "user" -> suspect.value,
      "atoms.reason" -> reason
    )

  def deleteAllBy(u: User) = for
    reports <- coll.list[Report]($doc("atoms.by" -> u.id), 500)
    _ <- reports.traverse: r =>
      val newAtoms = r.atoms.map: a =>
        if a.by.is(u)
        then a.copy(by = UserId.ghost.into(ReporterId))
        else a
      coll.update.one($id(r.id), $set("atoms" -> newAtoms))
    _ <- u.marks.clean.so:
      coll.update.one($doc("user" -> u.id), $set("user" -> UserId.ghost)).void
  yield ()

  object inquiries:

    private val workQueue = scalalib.actor.AsyncActorSequencer(
      maxSize = Max(32),
      timeout = 20.seconds,
      name = "report.inquiries",
      lila.log.asyncActorMonitor.full
    )

    def allBySuspect: Fu[Map[UserId, Report.Inquiry]] =
      coll
        .list[Report]($doc("inquiry.mod".$exists(true)))
        .map:
          _.view
            .flatMap: r =>
              r.inquiry.map: i =>
                r.user -> i
            .toMap

    def ofModId[U: UserIdOf](mod: U): Fu[Option[Report]] = coll.one[Report]($doc("inquiry.mod" -> mod.id))

    def ofSuspectId(suspectId: UserId): Fu[Option[Report.Inquiry]] =
      coll.primitiveOne[Report.Inquiry]($doc("inquiry.mod".$exists(true), "user" -> suspectId), "inquiry")

    def ongoingAppealOf(suspectId: UserId): Fu[Option[Report.Inquiry]] =
      coll.primitiveOne[Report.Inquiry](
        $doc(
          "inquiry.mod".$exists(true),
          "user" -> suspectId,
          "room" -> Room.Other.key,
          "atoms.0.text" -> Report.appealText
        ),
        "inquiry"
      )

    /*
     * If the mod has no current inquiry, just start this one.
     * If they had another inquiry, cancel it and start this one instead.
     * If they already are on this inquiry, and onlyOpen=false, cancel it.
     * Returns the previous and next inquiries
     */
    def toggle(id: String | Either[ReportId, UserId], onlyOpen: Boolean = false)(using
        Me
    ): Fu[(Option[Report], Option[Report])] =
      workQueue:
        doToggle(id, onlyOpen)

    private def doToggle(
        id: String | Either[ReportId, UserId],
        onlyOpen: Boolean
    )(using mod: Me): Fu[(Option[Report], Option[Report])] =
      def findByUser(userId: UserId) = coll.one[Report]($doc("user" -> userId, "inquiry.mod".$exists(true)))
      for
        report <- id match
          case Left(reportId) => coll.byId[Report](reportId)
          case Right(userId) => findByUser(userId)
          case anyId: String => coll.byId[Report](anyId).orElse(findByUser(UserId(anyId)))
        current <- ofModId(mod.userId)
        _ <- current.ifFalse(onlyOpen).so(cancel)
        _ <-
          report.so: r =>
            r.inquiry.isEmpty.so(
              coll
                .updateField(
                  $id(r.id),
                  "inquiry",
                  Report.Inquiry(mod.userId, nowInstant)
                )
                .void
            )
      yield (current, report.filter(_.inquiry.isEmpty || onlyOpen))

    def toggleNext(room: Room)(using Me): Fu[Option[Report]] =
      workQueue:
        findNext(room).flatMapz { report =>
          doToggle(Left(report.id), onlyOpen = false).dmap(_._2)
        }

    private def cancel(report: Report)(using mod: Me): Funit =
      if report.is(_.Other) && mod.is(report.onlyAtom.map(_.by))
      then coll.delete.one($id(report.id)).void // cancel spontaneous inquiry or appeal
      else
        coll.update
          .one(
            $id(report.id),
            $unset("inquiry", "done") ++ $set("open" -> true)
          )
          .void

    def spontaneous(sus: Suspect)(using Me): Fu[Report] =
      openOther(sus, Report.spontaneousText)

    def appeal(sus: Suspect)(using Me): Fu[Report] =
      openOther(sus, Report.appealText)

    def myUsernameReportText(using me: Me): Fu[Option[String]] =
      ofModId(me).map: report =>
        Report.Atom
          .best(report.so(_.atoms.toList).filter(_.is(_.Username)), 1)
          .headOption
          .map(_.textWithoutAutoReports)

    private def openOther(sus: Suspect, name: String)(using mod: Me): Fu[Report] =
      ofModId(mod.userId).flatMap: current =>
        current.so(cancel) >> {
          val report = Report
            .make(
              Candidate(
                Reporter(mod.value),
                sus,
                Reason.Other,
                name
              ).scored(Report.Score(0)),
              none
            )
            .copy(inquiry = Report.Inquiry(mod.userId, nowInstant).some)
          coll.insert.one(report).inject(report)
        }

    private[report] def expire: Funit =
      workQueue:
        val selector = $doc(
          "inquiry.mod".$exists(true),
          "inquiry.seenAt".$lt(nowInstant.minusMinutes(20))
        )
        coll.delete.one(selector ++ $doc("text" -> Report.spontaneousText)) >>
          coll.update.one(selector, $unset("inquiry"), multi = true).void
