package lila.relay

import chess.format.pgn.{ PgnStr, San, Std, Tags }
import chess.{ ErrorStr, Replay, Square, TournamentClock }
import scalalib.actor.AsyncActorSequencers
import com.github.blemale.scaffeine.LoadingCache

import lila.tree.{ ImportResult, ParseImport }
import lila.study.{ ChapterPreviewApi, MultiPgn, StudyPgnImport }
import lila.core.net.UserAgent
import lila.relay.RelayPush.*
import lila.memo.CacheApi

final class RelayPush(
    sync: RelaySync,
    api: RelayApi,
    chapterPreview: ChapterPreviewApi,
    fidePlayers: RelayFidePlayerApi,
    playerEnrich: RelayPlayerEnrich,
    irc: lila.core.irc.IrcApi
)(using Executor)(using scheduler: Scheduler):

  private val workQueue = AsyncActorSequencers[RelayRoundId](
    maxSize = Max(32),
    expiration = 1.minute,
    timeout = 10.seconds,
    name = "relay.push",
    lila.log.asyncActorMonitor.full
  )

  def apply(rt: RelayRound.WithTour, pgn: PgnStr)(using Me, UserAgent): Fu[Results] =
    push(rt, pgn).addEffect(monitor(rt))

  private def push(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Results] =
    cantHaveUpstream(rt.round) match
      case Some(failure) => fuccess(List(Left(failure)))
      case None =>
        val parsed = pgnToGames(pgn, rt.tour.info.clock)
        val games = parsed.collect { case Right(g) => g }.toVector
        val response: List[Either[Failure, Success]] =
          parsed.map(_.map(g => Success(g.tags, g.root.mainline.size)))

        rt.round.sync.delayMinusLag
          .ifTrue(games.exists(_.root.children.nonEmpty))
          .match
            case None =>
              push(rt, games).inject(response)
            case Some(delay) =>
              scheduler.scheduleOnce(delay.value.seconds):
                push(rt, games)
              fuccess(response)

  private def monitor(rt: RelayRound.WithTour)(results: Results)(using me: Me, ua: UserAgent): Unit =
    val client = ua.value.some
      .filter(_.startsWith("Lichess Broadcaster"))
      .flatMap(_.split("as:").headOption)
      .getOrElse(ua.value)
      .trim
      .some
      .filter(_.nonEmpty) | "no-UA"
    lila.mon.relay.push(name = rt.path, user = me.username, client = client)(
      games = results.size,
      moves = results.collect { case Right(a) => a.moves }.sum,
      errors = results.count(_.isLeft)
    )

  private def push(prev: RelayRound.WithTour, rawGames: Vector[RelayGame]) =
    workQueue(prev.round.id):
      for
        rt <- api.byIdWithTour(prev.round.id).orFail(s"Relay $prev no longer available")
        _ <- cantHaveUpstream(rt.round).so(fail => fufail[Unit](fail.error))
        withPlayers = playerEnrich.enrichAndReportAmbiguous(rt)(rawGames)
        withFide <- fidePlayers.enrichGames(rt.tour)(withPlayers)
        withReplacements = rt.tour.players.fold(withFide)(_.parse.update(withFide)._1)
        games = rt.tour.teams.fold(withReplacements)(_.update(withReplacements))
        event <- sync
          .updateStudyChapters(rt, games)
          .map: res =>
            SyncLog.event(res.nbMoves, none)
          .recover:
            case e: Exception => SyncLog.event(0, e.some)
        _ = if !rt.round.hasStarted && !rt.tour.official && event.hasMoves then
          irc.broadcastStart(rt.round.id, rt.fullNameNoTrans)
        allGamesFinished <- (games.nonEmpty && games.forall(_.points.isDefined)).so:
          chapterPreview.dataList(rt.round.studyId).map(_.forall(_.finished))
        round <- api.update(rt.round): r1 =>
          val r2 = r1.withSync(_.addLog(event))
          val r3 = if event.hasMoves then r2.ensureStarted.resume(rt.tour.official) else r2
          val finishedAt = allGamesFinished.option(r3.finishedAt.|(nowInstant))
          r3.copy(finishedAt = finishedAt)
        _ <- games.nonEmpty.so(api.syncTargetsOfSource(round))
      yield ()

  private val pgnCache: LoadingCache[PgnStr, Either[Failure, RelayGame]] =
    CacheApi.scaffeineNoScheduler
      .expireAfterAccess(2.minutes)
      .initialCapacity(1024)
      .maximumSize(4096)
      .build: pgn =>
        validate(pgn).map: importResult =>
          RelayGame.fromStudyImport(StudyPgnImport.result(importResult, Nil))

  private def pgnToGames(pgnBody: PgnStr, tc: Option[TournamentClock]): List[Either[Failure, RelayGame]] =
    RelayFetch.injectTimeControl
      .in(tc)(MultiPgn.split(pgnBody, RelayFetch.maxChaptersToShow))
      .value
      .map(pgnCache.get)

  private def cantHaveUpstream(round: RelayRound): Option[Failure] =
    round.sync.hasUpstream.option:
      Failure(Tags.empty, "The relay has an upstream URL, and cannot be pushed to.")

object RelayPush:

  case class Failure(tags: Tags, error: String)
  case class Success(tags: Tags, moves: Int)
  type Results = List[Either[Failure, Success]]

  // silently consume DGT board king-check move to center at game end
  private[relay] def validate(pgnBody: PgnStr): Either[Failure, ImportResult] =
    ParseImport
      .full(pgnBody)
      .fold(
        err => Failure(Tags.empty, err.value).asLeft,
        result =>
          val mainline = result.parsed.mainline
          result.replayError.fold(result.asRight): err =>
            mainline.lastOption match
              case Some(mv: Std) if isFatal(mv, result.replay, mainline) =>
                Failure(result.parsed.tags, err.value).asLeft
              case _ => result.asRight
      )

  private def isFatal(mv: Std, replay: Replay, parsed: List[San]) =
    import Square.*
    replay.moves.size < parsed.size - 1
    || mv.role != chess.King
    || (mv.dest != D4 && mv.dest != D5 && mv.dest != E4 && mv.dest != E5)
