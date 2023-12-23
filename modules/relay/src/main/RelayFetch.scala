package lila.relay

import akka.actor.*
import chess.Ply
import chess.format.pgn.{ Tags, SanStr, PgnStr }
import com.github.blemale.scaffeine.LoadingCache
import io.mola.galimatias.URL
import play.api.libs.json.*

import lila.base.LilaInvalid
import lila.common.{ Seconds, LilaScheduler }
import lila.game.{ GameRepo, PgnDump }
import lila.memo.CacheApi
import lila.round.GameProxyRepo
import lila.study.MultiPgn
import lila.tree.Node.Comments
import RelayRound.Sync.{ UpstreamIds, UpstreamUrl }
import RelayFormat.CanProxy
import lila.common.config.Max

final private class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    irc: lila.irc.IrcApi,
    formatApi: RelayFormatApi,
    delayer: RelayDelay,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    gameProxy: GameProxyRepo
)(using Executor, Scheduler):

  import RelayFetch.*

  LilaScheduler("RelayFetch.official", _.Every(500 millis), _.AtMost(15 seconds), _.Delay(15 seconds)):
    syncRelays(official = true)

  LilaScheduler("RelayFetch.user", _.Every(750 millis), _.AtMost(10 seconds), _.Delay(33 seconds)):
    syncRelays(official = false)

  private val maxRelaysToSync = Max(50)

  private def syncRelays(official: Boolean): Funit =
    val relays = if official then api.toSyncOfficial(maxRelaysToSync) else api.toSyncUser(maxRelaysToSync)
    relays
      .flatMap: relays =>
        lila.mon.relay.ongoing(official).update(relays.size)
        relays
          .map: rt =>
            if rt.round.sync.ongoing then
              processRelay(rt) flatMap: updating =>
                api.reFetchAndUpdate(rt.round)(updating.reRun)
            else if rt.round.hasStarted then
              logger.info(s"Finish by lack of activity ${rt.round}")
              api.update(rt.round)(_.finish)
            else if rt.round.shouldGiveUp then
              val msg = "Finish for lack of start"
              logger.info(s"$msg ${rt.round}")
              if rt.tour.official then irc.broadcastError(rt.round.id, rt.fullName, msg)
              api.update(rt.round)(_.finish)
            else funit
          .parallel
          .void

  // no writing the relay; only reading!
  // this can take a long time if the source is slow
  private def processRelay(rt: RelayRound.WithTour): Fu[Updating[RelayRound]] =
    val updating = Updating(rt.round)
    if !rt.round.sync.playing then fuccess(updating(_.withSync(_.play)))
    else
      fetchGames(rt)
        .map(games => rt.tour.players.fold(games)(_ update games))
        .mon(_.relay.fetchTime(rt.tour.official, rt.round.slug))
        .addEffect(gs => lila.mon.relay.games(rt.tour.official, rt.round.slug).update(gs.size))
        .flatMap: games =>
          sync
            .updateStudyChapters(rt, games)
            .withTimeoutError(7 seconds, SyncResult.Timeout)
            .mon(_.relay.syncTime(rt.tour.official, rt.round.slug))
            .map: res =>
              res -> updating:
                _.withSync(_ addLog SyncLog.event(res.nbMoves, none))
                  .copy(finished = games.nonEmpty && games.forall(_.ending.isDefined))
        .recover:
          case e: Exception =>
            val result = e.match
              case SyncResult.Timeout =>
                if rt.tour.official then logger.info(s"Sync timeout ${rt.round}")
                SyncResult.Timeout
              case _ =>
                if rt.tour.official then logger.info(s"Sync error ${rt.round} ${e.getMessage take 80}")
                SyncResult.Error(e.getMessage)
            result -> updating:
              _.withSync(_ addLog SyncLog.event(0, e.some))
        .map: (result, updatingRelay) =>
          afterSync(result, rt.tour, updatingRelay)

  private def afterSync(
      result: SyncResult,
      tour: RelayTour,
      updating: Updating[RelayRound]
  ): Updating[RelayRound] =
    val round = updating.current
    result match
      case result: SyncResult.Ok if result.nbMoves > 0 =>
        lila.mon.relay.moves(tour.official, round.slug).increment(result.nbMoves)
        if !round.hasStarted && !tour.official then
          irc.broadcastStart(round.id, round.withTour(tour).fullName)
        continueRelay(tour, updating(_.ensureStarted.resume))
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
            .filterNot(_ contains "Cannot parse moves")
            .filterNot(_ contains "Found an empty PGN")
            .foreach { irc.broadcastError(round.id, round.withTour(tour).fullName, _) }
          Seconds(60)
        else
          round.sync.period | Seconds:
            if upstream.local then 3
            else if upstream.asUrl.exists(_.isLcc) && !tour.official then 10
            else 5
      updating:
        _.withSync:
          _.copy(
            nextAt = nowInstant plusSeconds {
              seconds.atLeast {
                if round.sync.log.justTimedOut then 10 else 2
              }.value
            } some
          )

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
    rt.round.sync.upstream.so:
      case UpstreamIds(ids) =>
        gameRepo.gamesFromSecondary(ids) flatMap
          gameProxy.upgradeIfPresent flatMap
          gameRepo.withInitialFens flatMap { games =>
            if games.size == ids.size then
              val pgnFlags = gameIdsUpstreamPgnFlags.copy(delayMoves = !rt.tour.official)
              games
                .traverse: (game, fen) =>
                  pgnDump(game, fen, pgnFlags).dmap(_.render)
                .dmap(MultiPgn.apply)
            else
              throw LilaInvalid:
                s"Invalid game IDs: ${ids.filter(id => !games.exists(_._1.id == id)) mkString ", "}"
          } flatMap:
            multiPgnToGames(_).toFuture
      case url: UpstreamUrl =>
        delayer(url, rt, fetchFromUpstream(using CanProxy(rt.tour.official)))

  private def fetchFromUpstream(using canProxy: CanProxy)(upstream: UpstreamUrl, max: Max): Fu[RelayGames] =
    import DgtJson.*
    formatApi get upstream.withRound flatMap {
      case RelayFormat.SingleFile(doc) =>
        doc.format match
          // all games in a single PGN file
          case RelayFormat.DocFormat.Pgn => httpGetPgn(doc.url) map { MultiPgn.split(_, max) }
          // maybe a single JSON game? Why not
          case RelayFormat.DocFormat.Json =>
            httpGetJson[GameJson](doc.url) map: game =>
              MultiPgn(List(game.toPgn()))
      case RelayFormat.ManyFiles(indexUrl, makeGameDoc) =>
        httpGetJson[RoundJson](indexUrl) flatMap: round =>
          round.pairings.zipWithIndex
            .map: (pairing, i) =>
              val number  = i + 1
              val gameDoc = makeGameDoc(number)
              gameDoc.format
                .match
                  case RelayFormat.DocFormat.Pgn => httpGetPgn(gameDoc.url)
                  case RelayFormat.DocFormat.Json =>
                    httpGetJson[GameJson](gameDoc.url).recover { case _: Exception =>
                      GameJson(moves = Nil, result = none)
                    } map { _.toPgn(pairing.tags) }
                .recover: _ =>
                  PgnStr(s"${pairing.tags}\n\n${pairing.result}")
                .map(number -> _)
            .parallel
            .map: results =>
              MultiPgn(results.sortBy(_._1).map(_._2))
    } flatMap { multiPgnToGames(_).toFuture }

  private def httpGetPgn(url: URL)(using CanProxy): Fu[PgnStr] = PgnStr from formatApi.httpGet(url)

  private def httpGetJson[A: Reads](url: URL)(using CanProxy): Fu[A] = for
    str  <- formatApi.httpGet(url)
    json <- Future(Json parse str) // Json.parse throws exceptions (!)
    data <- summon[Reads[A]].reads(json).fold(err => fufail(s"Invalid JSON from $url: $err"), fuccess)
  yield data

private object RelayFetch:

  def maxChapters(tour: RelayTour) = Max:
    lila.study.Study.maxChapters * (if tour.official then 2 else 1)

  private[relay] object DgtJson:
    case class PairingPlayer(
        fname: Option[String],
        mname: Option[String],
        lname: Option[String],
        title: Option[String]
    ):
      def fullName = some {
        List(fname, mname, lname).flatten mkString " "
      }.filter(_.nonEmpty)
    case class RoundJsonPairing(white: PairingPlayer, black: PairingPlayer, result: String):
      import chess.format.pgn.*
      def tags = Tags:
        List(
          white.fullName map { Tag(_.White, _) },
          white.title map { Tag(_.WhiteTitle, _) },
          black.fullName map { Tag(_.Black, _) },
          black.title map { Tag(_.BlackTitle, _) },
          Tag(_.Result, result).some
        ).flatten
    case class RoundJson(pairings: List[RoundJsonPairing])
    given Reads[PairingPlayer]    = Json.reads
    given Reads[RoundJsonPairing] = Json.reads
    given Reads[RoundJson]        = Json.reads

    case class GameJson(moves: List[String], result: Option[String]):
      def toPgn(extraTags: Tags = Tags.empty): PgnStr =
        val strMoves = moves
          .map(_ split ' ')
          .mapWithIndex: (move, index) =>
            chess.format.pgn
              .Move(
                ply = Ply(index + 1),
                san = SanStr(~move.headOption),
                secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)) flatMap (_.toIntOption)
              )
              .render
          .mkString(" ")
        PgnStr(s"$extraTags\n\n$strMoves")
    given Reads[GameJson] = Json.reads

  object multiPgnToGames:

    def apply(multiPgn: MultiPgn): Either[LilaInvalid, Vector[RelayGame]] =
      multiPgn.value
        .foldLeftM(Vector.empty[RelayGame] -> 0):
          case ((acc, index), pgn) =>
            pgnCache
              .get(pgn)
              .flatMap: f =>
                val game = f(index)
                if game.isEmpty then LilaInvalid(s"Found an empty PGN at index $index").asLeft
                else (acc :+ game, index + 1).asRight[LilaInvalid]
        .map(_._1)

    private val pgnCache: LoadingCache[PgnStr, Either[LilaInvalid, Int => RelayGame]] =
      CacheApi.scaffeineNoScheduler
        .expireAfterAccess(2 minutes)
        .maximumSize(512)
        .build(compute)

    private def compute(pgn: PgnStr): Either[LilaInvalid, Int => RelayGame] =
      lila.study
        .PgnImport(pgn, Nil)
        .leftMap(err => LilaInvalid(err.value))
        .map: res =>
          index =>
            RelayGame(
              index = index,
              tags = res.tags,
              variant = res.variant,
              root = res.root.copy(
                comments = Comments.empty,
                children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
              ),
              ending = res.end
            )
