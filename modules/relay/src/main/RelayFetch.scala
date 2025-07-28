package lila.relay

import chess.format.pgn.{ PgnStr, Tag, Tags }
import chess.TournamentClock
import com.github.blemale.scaffeine.LoadingCache
import io.mola.galimatias.URL
import play.api.libs.json.*
import scalalib.model.Seconds

import lila.common.LilaScheduler
import lila.core.lilaism.LilaInvalid
import lila.game.{ GameRepo, PgnDump }
import lila.memo.CacheApi
import lila.relay.RelayRound.Sync
import lila.study.{ MultiPgn, StudyPgnImport }

final private class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    irc: lila.core.irc.IrcApi,
    http: HttpClient,
    formatApi: RelayFormatApi,
    delayer: RelayDelay,
    fidePlayers: RelayFidePlayerApi,
    gameRepo: GameRepo,
    studyChapterRepo: lila.study.ChapterRepo,
    pgnDump: PgnDump,
    gameProxy: lila.core.game.GameProxy,
    cacheApi: CacheApi,
    playerEnrich: RelayPlayerEnrich,
    notifyAdmin: RelayNotifierAdmin,
    onlyIds: Option[List[RelayTourId]] = None
)(using Executor, Scheduler)(using mode: play.api.Mode):

  import RelayFetch.*

  LilaScheduler(
    "RelayFetch.official",
    _.Every(if mode.isDev then 2.seconds else 500.millis),
    _.AtMost(15.seconds),
    _.Delay(if mode.isDev then 1.second else 21.seconds)
  ):
    syncRelays(official = true)

  LilaScheduler(
    "RelayFetch.user",
    _.Every(if mode.isDev then 2.seconds else 879.millis),
    _.AtMost(10.seconds),
    _.Delay(if mode.isDev then 2.second else 33.seconds)
  ):
    syncRelays(official = false)

  private val maxRelaysToSync = Max(50)

  private def syncRelays(official: Boolean): Funit = for
    relays <-
      if official then api.toSyncOfficial(maxRelaysToSync, onlyIds)
      else api.toSyncUser(maxRelaysToSync, onlyIds)
    _ <- relays.parallelVoid(syncRelay)
  yield lila.mon.relay.ongoing(official).update(relays.size)

  private def syncRelay(rt: RelayRound.WithTour): Funit =
    if rt.round.sync.ongoing then
      processRelay(rt).flatMap: updating =>
        api.reFetchAndUpdate(rt.round)(updating.reRun).void
    else if rt.round.hasStarted then
      logger.info(s"Finish by lack of activity ${rt.round}")
      api.update(rt.round)(_.finish).void
    else if rt.round.shouldGiveUp then
      val msg = "Finish for lack of start"
      logger.info(s"$msg ${rt.round}")
      if rt.tour.official then irc.broadcastError(rt.round.id, rt.fullName, msg)
      api.update(rt.round)(_.finish).void
    else
      logger.info(s"Pause sync until round starts ${rt.round}")
      api.update(rt.round)(_.withSync(_.pause)).void

  // no writing the relay; only reading!
  // this can take a long time if the source is slow
  private def processRelay(rt: RelayRound.WithTour): Fu[Updating[RelayRound]] =
    val updating = Updating(rt.round)
    if !rt.round.sync.playing then fuccess(updating(_.withSync(_.play(rt.tour.official))))
    else
      val syncFu = for
        allGamesInSourceNoLimit <- fetchGames(rt).mon:
          _.relay.fetchTime(rt.tour.official, rt.tour.id, rt.tour.slug)
        allGamesInSource = allGamesInSourceNoLimit.take(maxGamesToRead(rt.tour.official).value)
        filtered = RelayGame.filter(rt.round.sync.onlyRound)(allGamesInSource)
        sliced = RelayGame.Slices.filterAndOrder(~rt.round.sync.slices)(filtered)
        limited = sliced.take(RelayFetch.maxChaptersToShow.value)
        _ <- (sliced.sizeCompare(limited) != 0 && rt.tour.official)
          .so(notifyAdmin.tooManyGames(rt, sliced.size, RelayFetch.maxChaptersToShow))
        withPlayers <- playerEnrich.enrichAndReportAmbiguous(rt)(limited)
        withFide <- fidePlayers.enrichGames(rt.tour)(withPlayers)
        withTeams = rt.tour.teams.fold(withFide)(_.update(withFide))
        res <- sync
          .updateStudyChapters(rt, withTeams)
          .withTimeoutError(7.seconds, SyncResult.Timeout)
          .mon(_.relay.syncTime(rt.tour.official, rt.tour.id, rt.tour.slug))
        games = res.plan.input.games
        _ <- notifyAdmin.orphanBoards.inspectPlan(rt, res.plan)
        nbGamesFinished = games.count(_.points.isDefined)
        nbGamesUnstarted = games.count(!_.hasMoves)
        allGamesFinishedOrUnstarted = games.nonEmpty &&
          nbGamesFinished + nbGamesUnstarted >= games.size &&
          nbGamesFinished > nbGamesUnstarted
        noMoreGamesSelected = games.isEmpty && allGamesInSource.nonEmpty
        autoFinishNow = rt.round.hasStarted && (allGamesFinishedOrUnstarted || noMoreGamesSelected)
        roundUpdate = updating: r =>
          r.withSync(_.addLog(SyncLog.event(res.nbMoves, none)))
            .copy(finishedAt = r.finishedAt.orElse(autoFinishNow.option(nowInstant)))
      yield res -> roundUpdate
      syncFu
        .recover:
          case e: Exception =>
            val result = e.match
              case e @ LilaInvalid(msg) =>
                logger.info(s"Sync fail ${rt.round} $msg")
                SyncResult.Error(msg)
              case SyncResult.Timeout =>
                if rt.tour.official then logger.info(s"Sync timeout ${rt.round}")
                SyncResult.Timeout
              case _ =>
                if rt.tour.official then logger.info(s"Sync error ${rt.round} ${e.getMessage.take(80)}")
                SyncResult.Error(e.getMessage)
            result -> updating:
              _.withSync(_.addLog(SyncLog.event(0, e.some)))
        .map: (result, updatingRelay) =>
          afterSync(result, rt.tour, updatingRelay)

  private def afterSync(
      result: SyncResult,
      tour: RelayTour,
      updating: Updating[RelayRound]
  ): Updating[RelayRound] =
    val round = updating.current
    result match
      case result: SyncResult.Ok if result.hasMovesOrTags =>
        api.syncTargetsOfSource(round)
        if result.nbMoves > 0 then
          lila.mon.relay.moves(tour.official, tour.id, tour.slug).increment(result.nbMoves)
          if tour.official then notifyAdmin.missingFideIds.schedule(round.id)
          if !round.hasStarted && !tour.official
          then irc.broadcastStart(round.id, round.withTour(tour).fullName)
          continueRelay(tour, updating(_.ensureStarted.resume(tour.official)))
        else continueRelay(tour, updating)
      case _ => continueRelay(tour, updating)

  private def continueRelay(tour: RelayTour, updating: Updating[RelayRound]): Updating[RelayRound] =
    val round = updating.current
    round.sync.upstream.fold(updating): upstream =>
      reportBroadcastFailure(round.withTour(tour))
      val seconds: Seconds =
        if round.sync.log.alwaysFails then
          Seconds(tour.tier.fold(60):
            case RelayTour.Tier.best => 10
            case RelayTour.Tier.high => 20
            case _ => 40)
        else round.sync.period | dynamicPeriod(tour, round, upstream)
      updating:
        _.withSync:
          _.copy(
            nextAt = nowInstant.plusSeconds {
              seconds.atLeast(if round.sync.log.justTimedOut then 10 else 2).value
            }.some
          )

  private def reportBroadcastFailure(r: RelayRound.WithTour): Unit =
    if r.round.sync.log.alwaysFails && r.tour.official && r.round.shouldHaveStarted then
      r.round.sync.log.events.lastOption
        .filterNot(_.isTimeout)
        .flatMap(_.error)
        .filterNot(_.contains("Error parsing move"))
        .filterNot(_.contains("Error parsing PGN"))
        .filterNot(_.contains("Found an empty PGN"))
        .foreach { irc.broadcastError(r.round.id, r.fullName, _) }

  private def dynamicPeriod(tour: RelayTour, round: RelayRound, upstream: Sync.Upstream) = Seconds:
    val base =
      if upstream.isInternal then 1
      else if upstream.hasLcc then 4
      else if upstream.isRound then 10 // uses push so no need to pull often
      else 2
    base * {
      if tour.tierIs(_.best) then 1
      else if tour.official then 2
      else 3
    } * {
      if upstream.hasLcc && !tour.tierIs(_.best) && round.crowd.exists(_ < Crowd(10)) then 2 else 1
    } * {
      if round.hasStarted then 1
      else if round.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(20))) then 2
      else 3
    }

  private val gameIdsUpstreamPgnFlags = PgnDump.WithFlags(
    clocks = true,
    moves = true,
    tags = true,
    evals = false,
    opening = false,
    literate = false,
    pgnInJson = false,
    delayMoves = true
  )

  private def fetchGames(rt: RelayRound.WithTour): Fu[RelayGames] =
    given CanProxy = CanProxy(rt.tour.official)
    rt.round.sync.upstream.so:
      case Sync.Upstream.Ids(ids) => delayer.internalSource(rt.round, fetchFromGameIds(rt.tour, ids))
      case Sync.Upstream.Users(users) => delayer.internalSource(rt.round, fetchFromUsers(rt.tour, users))
      case Sync.Upstream.Url(url) => delayer.urlSource(url, rt.round, fetchFromUpstream(rt))
      case Sync.Upstream.Urls(urls) =>
        urls.toVector
          .parallel: url =>
            delayer.urlSource(url, rt.round, fetchFromUpstreamWithRecovery(rt))
          .map(_.flatten)

  private def fetchFromGameIds(tour: RelayTour, ids: List[GameId]): Fu[RelayGames] =
    gameRepo
      .gamesFromSecondary(ids)
      .flatMap: games =>
        if games.sizeIs == ids.size then fromLichessGames(tour)(games)
        else
          fufail:
            LilaInvalid:
              s"Invalid game IDs: ${ids.filter(id => !games.exists(_.id == id)).mkString(", ")}"

  // remembers previously ongoing games so they can be fetched one last time when finished
  private val ongoingUserGameIdsCache =
    cacheApi.notLoadingSync[RelayTourId, Set[GameId]](16, "relay.fetch.ongoingUserGameIds"):
      _.expireAfterWrite(15.minutes).build()

  private def fetchFromUsers(tour: RelayTour, users: List[UserStr]): Fu[RelayGames] =
    val ids = users.map(_.id).toSet
    (ids.sizeIs > 1).so:
      for
        ongoingGames <- gameRepo.ongoingByUserIdsCursor(ids).collect[List](ids.size)
        ongoingIds = ongoingGames.map(_.id).toSet
        prevGameIds = ~ongoingUserGameIdsCache.getIfPresent(tour.id)
        recentlyFinishedIds = prevGameIds.diff(ongoingIds)
        recentlyFinished <- gameRepo.gamesFromSecondary(recentlyFinishedIds.toSeq)
        allGames = recentlyFinished ++ ongoingGames
        _ = ongoingUserGameIdsCache.put(tour.id, ongoingIds)
        games <- fromLichessGames(tour)(allGames)
      yield games

  private def fromLichessGames(tour: RelayTour)(dbGames: List[lila.core.game.Game]): Fu[RelayGames] = for
    upgraded <- gameProxy.upgradeIfPresent(dbGames)
    withFen <- gameRepo.withInitialFens(upgraded)
    pgnFlags = gameIdsUpstreamPgnFlags.copy(delayMoves = !tour.official)
    pgn <- withFen.sequentially((game, fen) => pgnDump(game, fen, pgnFlags).map(_.render))
    games <- multiPgnToGames.future(MultiPgn(pgn))
  yield games

  private object lccCache:
    import DgtJson.GameJson
    type LccGameKey = String
    // cache finished games so they're not requested again for a while
    private val finishedGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](512, "relay.fetch.finishedLccGames"):
        _.expireAfterWrite(8.minutes).build()
    // cache created (non-started) games until they start
    private val createdGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](256, "relay.fetch.createdLccGames"):
        _.expireAfter[LccGameKey, GameJson](
          create = (key, _) => (if key.startsWith("started ") then 20.seconds else 3.minutes),
          update = (_, _, current) => current,
          read = (_, _, current) => current
        ).build()
    // cache games with number > 12 to reduce load on big tournaments
    val tailAt = 30
    private val tailGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](256, "relay.fetch.tailLccGames"):
        _.expireAfterWrite(1.minutes).build()

    // index starts at 1
    def apply(lcc: RelayRound.Sync.Lcc, index: Int, roundTags: Tags, started: Boolean)(
        fetch: () => Fu[GameJson]
    ): Fu[GameJson] =
      val key = s"${started.so("started ")}${lcc.id} ${lcc.round} $index"
      finishedGames
        .getIfPresent(key)
        .orElse(createdGames.getIfPresent(key))
        .orElse((index > lccCache.tailAt).so(tailGames.getIfPresent(key)))
        .match
          case Some(game) => fuccess(game)
          case None =>
            fetch().addEffect: game =>
              if game.moves.isEmpty then createdGames.put(key, game)
              else if game.mergeRoundTags(roundTags).outcome.isDefined then finishedGames.put(key, game)
              else if index > lccCache.tailAt then tailGames.put(key, game)

  // used to return the last successful result when a source fails
  // games are stripped of their moves, only tags are kept.
  // the point is to avoid messing up slices in multi-URL setups.
  // if a single URL fails, it should not moves the games of the following URLs.
  private val multiUrlFetchRecoverCache =
    cacheApi.notLoadingSync[URL, RelayGames](16, "relay.fetch.recoverCache"):
      _.expireAfterWrite(1.hour).build()

  private def fetchFromUpstreamWithRecovery(rt: RelayRound.WithTour)(url: URL)(using
      CanProxy
  ): Fu[RelayGames] =
    fetchFromUpstream(rt)(url)
      .addEffect: games =>
        multiUrlFetchRecoverCache.put(url, games.map(_.withoutMoves))
      .recover:
        case e: Exception =>
          logger.info(s"Fetch error in multi-url ${rt.round.id} $url ${e.getMessage.take(80)}", e)
          val recovery = multiUrlFetchRecoverCache.getIfPresent(url)
          logger.info:
            recovery.fold(s"No recovery found for $url")(r => s"Recovery found for $url with ${r.size} games")
          ~recovery

  private def fetchFromUpstream(rt: RelayRound.WithTour)(url: URL)(using CanProxy): Fu[RelayGames] =
    import DgtJson.*
    formatApi
      .get(url)
      .flatMap:
        case RelayFormat.Round(id) =>
          studyChapterRepo
            .orderedByStudyLoadingAllInMemory(id.into(StudyId))
            .map(_.view.map(RelayGame.fromChapter).toVector)
        case RelayFormat.SingleFile(url) =>
          httpGetPgn(url)
            .map(MultiPgn.split(_, RelayFetch.maxGamesToRead(rt.tour.official)))
            .map(injectTimeControl.in(rt.tour.info.clock))
            .flatMap(multiPgnToGames.future)
        case RelayFormat.LccWithGames(lcc) =>
          httpGetRoundJson(lcc.indexUrl)
            .flatMap: round =>
              val lookForStart: Boolean =
                rt.round.startsAtTime
                  .map(_.minusSeconds(rt.round.sync.delay.so(_.value) + 5 * 60))
                  .forall(_.isBeforeNow)
              round.pairings
                .mapWithIndex: (pairing, i) =>
                  val game = i + 1
                  val tags = pairing.tags(lcc.round, game, round.formattedDate)
                  lccCache(lcc, game, tags, lookForStart): () =>
                    httpGetGameJson(lcc.gameUrl(game)).recover:
                      case _: Exception => GameJson(moves = Nil, result = none)
                  .map { _.toPgn(tags) }
                    .recover: _ =>
                      PgnStr(s"${tags}\n\n${pairing.result}")
                    .map(game -> _)
                .parallel
                .map: pgns =>
                  MultiPgn(pgns.sortBy(_._1).map(_._2))
                .map(injectTimeControl.in(rt.tour.info.clock))
                .flatMap(multiPgnToGames.future)
        case RelayFormat.LccWithoutGames(lcc) =>
          httpGetRoundJson(lcc.indexUrl)
            .map: round =>
              MultiPgn:
                round.pairings.mapWithIndex: (pairing, i) =>
                  PgnStr(s"${pairing.tags(lcc.round, i + 1, round.formattedDate)}\n\n${pairing.result}")
            .map(injectTimeControl.in(rt.tour.info.clock))
            .flatMap(multiPgnToGames.future)

  private def httpGetPgn(url: URL)(using CanProxy): Fu[PgnStr] = PgnStr.from(http.get(url))
  private def httpGetRoundJson(url: URL)(using CanProxy): Fu[DgtJson.RoundJson] =
    http.get(url).flatMap(readAsJson[DgtJson.RoundJson](url))
  private def httpGetGameJson(url: URL)(using CanProxy): Fu[DgtJson.GameJson] =
    http.get(url).flatMap(readAsJson[DgtJson.GameJson](url))
  private def readAsJson[A: Reads](url: URL)(body: HttpClient.Body): Fu[A] = for
    json <- Future(Json.parse(body)) // Json.parse throws exceptions (!)
    data <- summon[Reads[A]].reads(json).fold(err => fufail(s"Invalid JSON from $url: $err"), fuccess)
  yield data

private object RelayFetch:

  val maxChaptersToShow: Max = Max(100)
  private val maxGamesToRead: Max = Max(256)
  private val maxGamesToReadOfficial: Max = maxGamesToRead.map(_ * 3)
  def maxGamesToRead(official: Boolean): Max = if official then maxGamesToReadOfficial else maxGamesToRead

  object injectTimeControl:

    private def replace(tc: TournamentClock): String = s"${Tag.timeControl(tc)}\n"

    def in(tco: Option[TournamentClock])(multiPgn: MultiPgn): MultiPgn =
      tco.fold(multiPgn): tc =>
        MultiPgn:
          multiPgn.value.map(in(tc))

    def in(tco: TournamentClock)(pgn: PgnStr): PgnStr =
      pgn.map: txt =>
        if txt.contains("""[TimeControl """")
        then txt
        else s"""${replace(tco)}$txt"""

  object multiPgnToGames:

    def either(multiPgn: MultiPgn): Either[LilaInvalid, Vector[RelayGame]] =
      multiPgn.value
        .foldLeftM(Vector.empty[RelayGame] -> 0):
          case ((acc, index), pgn) =>
            pgnCache
              .get(pgn)
              .flatMap: game =>
                if game.isEmpty then LilaInvalid(s"Found an empty PGN at index $index").asLeft
                else (acc :+ game, index + 1).asRight
        .map(_._1)

    def future(multiPgn: MultiPgn): Fu[Vector[RelayGame]] = either(multiPgn).toFuture

    private val pgnCache: LoadingCache[PgnStr, Either[LilaInvalid, RelayGame]] =
      CacheApi
        .scaffeineNoScheduler(using scala.concurrent.ExecutionContextOpportunistic)
        .expireAfterAccess(2.minutes)
        .initialCapacity(1024)
        .maximumSize(4096)
        .build(compute)

    private def compute(pgn: PgnStr): Either[LilaInvalid, RelayGame] =
      StudyPgnImport
        .result(pgn, Nil)
        .leftMap(err => LilaInvalid(err.value))
        .map(RelayGame.fromStudyImport)
