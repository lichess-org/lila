package lila.relay

import akka.actor.*
import chess.format.pgn.{ PgnStr, SanStr, Tag, Tags }
import chess.{ Outcome, Ply }
import com.github.blemale.scaffeine.LoadingCache
import io.mola.galimatias.URL
import play.api.libs.json.*
import scalalib.model.Seconds

import lila.common.LilaScheduler
import lila.core.lilaism.LilaInvalid
import lila.game.{ GameRepo, PgnDump }
import lila.memo.CacheApi
import lila.relay.RelayFormat.CanProxy
import lila.relay.RelayRound.Sync
import lila.study.{ MultiPgn, StudyPgnImport }
import lila.tree.Node.Comments

final private class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    irc: lila.core.irc.IrcApi,
    formatApi: RelayFormatApi,
    delayer: RelayDelay,
    fidePlayers: RelayFidePlayerApi,
    gameRepo: GameRepo,
    studyChapterRepo: lila.study.ChapterRepo,
    pgnDump: PgnDump,
    gameProxy: lila.core.game.GameProxy,
    cacheApi: CacheApi,
    playersApi: RelayPlayersApi,
    notifyMissingFideIds: RelayNotifyMissingFideIds,
    orphanNotifier: RelayNotifyOrphanBoard,
    onlyIds: Option[List[RelayTourId]] = None
)(using Executor, Scheduler, lila.core.i18n.Translator)(using mode: play.api.Mode):

  import RelayFetch.*

  LilaScheduler(
    "RelayFetch.official",
    _.Every(if mode.isDev then 2.seconds else 500 millis),
    _.AtMost(15 seconds),
    _.Delay(if mode.isDev then 1.second else 21 seconds)
  ):
    syncRelays(official = true)

  LilaScheduler(
    "RelayFetch.user",
    _.Every(if mode.isDev then 2.seconds else 879 millis),
    _.AtMost(10 seconds),
    _.Delay(if mode.isDev then 2.second else 33 seconds)
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
        allGamesInSource <- fetchGames(rt).mon(_.relay.fetchTime(rt.tour.official, rt.tour.id, rt.tour.slug))
        _ = lila.mon.relay.games(rt.tour.official, rt.tour.id, rt.round.slug).update(allGamesInSource.size)
        filtered = RelayGame.filter(rt.round.sync.onlyRound)(allGamesInSource)
        sliced   = RelayGame.Slices.filter(~rt.round.sync.slices)(filtered)
        withPlayers <- playersApi.updateAndReportAmbiguous(rt)(sliced)
        enriched    <- fidePlayers.enrichGames(rt.tour)(withPlayers)
        withTeams = rt.tour.teams.fold(enriched)(_.update(enriched))
        res <- sync
          .updateStudyChapters(rt, withTeams)
          .withTimeoutError(7 seconds, SyncResult.Timeout)
          .mon(_.relay.syncTime(rt.tour.official, rt.tour.id, rt.tour.slug))
        games = res.plan.input.games
        _ <- orphanNotifier.inspectPlan(rt, res.plan)
        allGamesHaveOutcome = games.nonEmpty && games.forall(_.outcome.isDefined)
        noMoreGamesSelected = games.isEmpty && allGamesInSource.nonEmpty && rt.round.startedAt.isDefined
        nextRoundToStart <- noMoreGamesSelected.so(api.nextRoundThatStartsAfterThisOneCompletes(rt.round))
      yield res -> updating:
        _.withSync(_.addLog(SyncLog.event(res.nbMoves, none)))
          .copy(finished = allGamesHaveOutcome || nextRoundToStart.isDefined)
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
          if tour.official then notifyMissingFideIds.schedule(round.id)
          if !round.hasStarted && !tour.official
          then irc.broadcastStart(round.id, round.withTour(tour).fullName)
          continueRelay(tour, updating(_.ensureStarted.resume(tour.official)))
        else continueRelay(tour, updating)
      case _ => continueRelay(tour, updating)

  private def continueRelay(tour: RelayTour, updating: Updating[RelayRound]): Updating[RelayRound] =
    val round = updating.current
    round.sync.upstream.fold(updating): upstream =>
      val seconds: Seconds =
        if round.sync.log.alwaysFails then
          round.sync.log.events.lastOption
            .filterNot(_.isTimeout)
            .flatMap(_.error)
            .ifTrue(tour.official && round.shouldHaveStarted)
            .filterNot(_.contains("Cannot parse moves"))
            .filterNot(_.contains("Found an empty PGN"))
            .foreach { irc.broadcastError(round.id, round.withTour(tour).fullName, _) }
          Seconds(60)
        else round.sync.period | dynamicPeriod(tour, round, upstream)
      updating:
        _.withSync:
          _.copy(
            nextAt = nowInstant.plusSeconds {
              seconds.atLeast {
                if round.sync.log.justTimedOut then 10 else 2
              }.value
            } some
          )

  private def dynamicPeriod(tour: RelayTour, round: RelayRound, upstream: Sync.Upstream) = Seconds:
    val base =
      if upstream.hasLcc then 6
      else if upstream.isRound then 10 // uses push so no need to pull oftenrelayfetch
      else 2
    base * {
      if tour.tier.exists(_ > RelayTour.Tier.NORMAL) then 1
      else if tour.official then 2
      else 3
    } * {
      if upstream.hasLcc && round.crowd.exists(_ < 10) then 2 else 1
    } * {
      if round.hasStarted then 1 else 2
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
      case Sync.Upstream.Ids(ids) => fetchFromGameIds(rt.tour, ids)
      case Sync.Upstream.Url(url) => delayer(url, rt.round, fetchFromUpstream(rt))
      case Sync.Upstream.Urls(urls) =>
        urls.toVector
          .parallel: url =>
            delayer(url, rt.round, fetchFromUpstreamWithRecover(rt))
          .map(_.flatten)

  private def fetchFromGameIds(tour: RelayTour, ids: List[GameId]): Fu[RelayGames] =
    gameRepo
      .gamesFromSecondary(ids)
      .flatMap(gameProxy.upgradeIfPresent)
      .flatMap(gameRepo.withInitialFens)
      .flatMap: games =>
        if games.sizeIs == ids.size then
          val pgnFlags             = gameIdsUpstreamPgnFlags.copy(delayMoves = !tour.official)
          given play.api.i18n.Lang = lila.core.i18n.defaultLang
          games
            .sequentially: (game, fen) =>
              pgnDump(game, fen, pgnFlags).dmap(_.render)
            .dmap(MultiPgn.apply)
        else
          throw LilaInvalid:
            s"Invalid game IDs: ${ids.filter(id => !games.exists(_._1.id == id)).mkString(", ")}"
      .flatMap(multiPgnToGames.future)

  private object lccCache:
    import DgtJson.GameJson
    type LccGameKey = String
    // cache finished games so they're not requested again for a while
    private val finishedGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](512, "relay.fetch.finishedLccGames"):
        _.expireAfterWrite(8 minutes).build()
    // cache created (non-started) games until they start
    private val createdGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](256, "relay.fetch.createdLccGames"):
        _.expireAfter[LccGameKey, GameJson](
          create = (key, _) => (if key.startsWith("started ") then 1 minute else 5 minutes),
          update = (_, _, current) => current,
          read = (_, _, current) => current
        ).build()
    // cache games with number > 12 to reduce load on big tournaments
    val tailAt = 12
    private val tailGames =
      cacheApi.notLoadingSync[LccGameKey, GameJson](256, "relay.fetch.tailLccGames"):
        _.expireAfterWrite(1 minutes).build()

    def apply(lcc: RelayRound.Sync.Lcc, index: Int, roundTags: Tags, started: Boolean)(
        fetch: () => Fu[GameJson]
    ): Fu[GameJson] =
      val key = s"${started.so("started ")}${lcc.id} ${lcc.round} $index"
      finishedGames
        .getIfPresent(key)
        .orElse(createdGames.getIfPresent(key))
        .orElse((index >= lccCache.tailAt).so(tailGames.getIfPresent(key)))
        .match
          case Some(game) => fuccess(game)
          case None =>
            fetch().addEffect: game =>
              if game.moves.isEmpty then createdGames.put(key, game)
              else if game.mergeRoundTags(roundTags).outcome.isDefined then finishedGames.put(key, game)
              else if index >= lccCache.tailAt then tailGames.put(key, game)

  private def fetchFromUpstreamWithRecover(rt: RelayRound.WithTour)(url: URL)(using
      CanProxy
  ): Fu[RelayGames] =
    fetchFromUpstream(rt)(url).recover:
      case e: Exception =>
        logger.info(s"Fetch error in multi-url ${rt.round.id} $url ${e.getMessage.take(80)}", e)
        Vector.empty

  private def fetchFromUpstream(rt: RelayRound.WithTour)(url: URL)(using CanProxy): Fu[RelayGames] =
    import DgtJson.*
    formatApi
      .get(url)
      .flatMap {
        case RelayFormat.Round(id) =>
          studyChapterRepo
            .orderedByStudyLoadingAllInMemory(id.into(StudyId))
            .map(_.view.map(RelayGame.fromChapter).toVector)
        case RelayFormat.SingleFile(url) =>
          httpGetPgn(url).map { MultiPgn.split(_, RelayFetch.maxChapters) }.flatMap(multiPgnToGames.future)
        case RelayFormat.LccWithGames(lcc) =>
          httpGetJson[RoundJson](lcc.indexUrl).flatMap: round =>
            val lookForStart: Boolean =
              rt.round.startsAtTime
                .map(_.minusSeconds(rt.round.sync.delay.so(_.value) + 5 * 60))
                .forall(_.isBeforeNow)
            round.pairings
              .mapWithIndex: (pairing, i) =>
                val game = i + 1
                val tags = pairing.tags(lcc.round, game, round.date)
                lccCache(lcc, game, tags, lookForStart): () =>
                  httpGetJson[GameJson](lcc.gameUrl(game)).recover:
                    case _: Exception => GameJson(moves = Nil, result = none)
                .map { _.toPgn(tags) }
                  .recover: _ =>
                    PgnStr(s"${tags}\n\n${pairing.result}")
                  .map(game -> _)
              .parallel
              .map: pgns =>
                MultiPgn(pgns.sortBy(_._1).map(_._2))
              .flatMap(multiPgnToGames.future)
        case RelayFormat.LccWithoutGames(lcc) =>
          httpGetJson[RoundJson](lcc.indexUrl)
            .map: round =>
              MultiPgn:
                round.pairings.mapWithIndex: (pairing, i) =>
                  PgnStr(s"${pairing.tags(lcc.round, i + 1, round.date)}\n\n${pairing.result}")
            .flatMap(multiPgnToGames.future)
      }

  private def httpGetPgn(url: URL)(using CanProxy): Fu[PgnStr] =
    PgnStr.from(formatApi.httpGetAndGuessCharset(url))

  private def httpGetJson[A: Reads](url: URL)(using CanProxy): Fu[A] = for
    str  <- formatApi.httpGet(url)
    json <- Future(Json.parse(str)) // Json.parse throws exceptions (!)
    data <- summon[Reads[A]].reads(json).fold(err => fufail(s"Invalid JSON from $url: $err"), fuccess)
  yield data

private object RelayFetch:

  export lila.study.Study.maxChapters

  private[relay] object DgtJson:
    case class PairingPlayer(
        fname: Option[String],
        mname: Option[String],
        lname: Option[String],
        title: Option[String],
        fideid: Option[Int]
    ):
      def fullName = some {
        List(fname, mname, lname).flatten.mkString(" ")
      }.filter(_.nonEmpty)
    case class RoundJsonPairing(
        white: Option[PairingPlayer],
        black: Option[PairingPlayer],
        result: Option[String]
    ):
      import chess.format.pgn.*
      def tags(round: Int, game: Int, date: Option[String]) = Tags:
        List(
          white.flatMap(_.fullName).map { Tag(_.White, _) },
          white.flatMap(_.title).map { Tag(_.WhiteTitle, _) },
          white.flatMap(_.fideid).map { Tag(_.WhiteFideId, _) },
          black.flatMap(_.fullName).map { Tag(_.Black, _) },
          black.flatMap(_.title).map { Tag(_.BlackTitle, _) },
          black.flatMap(_.fideid).map { Tag(_.BlackFideId, _) },
          result.map(Tag(_.Result, _)),
          Tag(_.Round, s"$round.$game").some,
          date.map(Tag(_.Date, _))
        ).flatten
    case class RoundJson(
        date: Option[String],
        pairings: List[RoundJsonPairing]
    ):
      def finishedGameIndexes: List[Int] = pairings.zipWithIndex.collect:
        case (pairing, i) if pairing.result.forall(_ != "*") => i
    given Reads[PairingPlayer]    = Json.reads
    given Reads[RoundJsonPairing] = Json.reads
    given Reads[RoundJson]        = Json.reads

    case class GameJson(moves: List[String], result: Option[String], chess960: Option[Int] = none):
      def outcome = result.flatMap(Outcome.fromResult)
      def mergeRoundTags(roundTags: Tags): Tags =
        val fenTag = chess960
          .filter(_ != 518) // LCC sends 518 for standard chess
          .flatMap(chess.variant.Chess960.positionToFen)
          .map(pos => Tag(_.FEN, pos.value))
        val outcomeTag = outcome.map(o => Tag(_.Result, Outcome.showResult(o.some)))
        roundTags ++ Tags(List(fenTag, outcomeTag).flatten)
      def toPgn(roundTags: Tags): PgnStr =
        val mergedTags = mergeRoundTags(roundTags)
        val strMoves = moves
          .map(_.split(' '))
          .map: move =>
            chess.format.pgn
              .Move(
                san = SanStr(~move.headOption),
                secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)).flatMap(_.toIntOption)
              )
              .render
          .mkString(" ")
        PgnStr(s"$mergedTags\n\n$strMoves")
    given Reads[GameJson] = Json.reads

  object multiPgnToGames:

    def apply(multiPgn: MultiPgn): Either[LilaInvalid, Vector[RelayGame]] =
      multiPgn.value
        .foldLeftM(Vector.empty[RelayGame] -> 0):
          case ((acc, index), pgn) =>
            pgnCache
              .get(pgn)
              .flatMap: game =>
                if game.isEmpty then LilaInvalid(s"Found an empty PGN at index $index").asLeft
                else (acc :+ game, index + 1).asRight[LilaInvalid]
        .map(_._1)

    def future(multiPgn: MultiPgn): Fu[Vector[RelayGame]] = apply(multiPgn).toFuture

    private val pgnCache: LoadingCache[PgnStr, Either[LilaInvalid, RelayGame]] =
      CacheApi
        .scaffeineNoScheduler(using scala.concurrent.ExecutionContextOpportunistic)
        .expireAfterAccess(2 minutes)
        .maximumSize(1024)
        .build(compute)

    private def compute(pgn: PgnStr): Either[LilaInvalid, RelayGame] =
      StudyPgnImport(pgn, Nil)
        .leftMap(err => LilaInvalid(err.value))
        .map: res =>
          val fixedTags = // remove wrong ongoing result tag if the board has a mate on it
            if res.end.isDefined && res.tags(_.Result).has("*") then
              Tags(res.tags.value.filter(_ != Tag(_.Result, "*")))
            else res.tags
          RelayGame(
            tags = fixedTags,
            variant = res.variant,
            root = res.root.copy(
              comments = Comments.empty,
              children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
            ),
            outcome = res.end.map(_.outcome)
          )
