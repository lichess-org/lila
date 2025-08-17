package lila.relay

import play.api.mvc.RequestHeader
import chess.format.pgn.{ PgnStr, San, Std, Tags }
import chess.{ ErrorStr, Replay, Square, TournamentClock }
import scalalib.actor.AsyncActorSequencers
import lila.tree.{ ImportResult, ParseImport }

import lila.study.{ ChapterPreviewApi, MultiPgn, StudyPgnImport }
import lila.common.HTTPRequest
import RelayPush.*

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

  def apply(rt: RelayRound.WithTour, pgn: PgnStr)(using Me, RequestHeader): Fu[Results] =
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
              for _ <- push(rt, games) yield response
            case Some(delay) =>
              scheduler.scheduleOnce(delay.value.seconds):
                push(rt, games)
              fuccess(response)

  private def monitor(rt: RelayRound.WithTour)(results: Results)(using me: Me, req: RequestHeader): Unit =
    val ua = HTTPRequest.userAgent(req)
    val client = ua
      .filter(_.value.startsWith("Lichess Broadcaster"))
      .flatMap(_.value.split("as:").headOption)
      .orElse(ua.map(_.value))
      .fold("unknown")(_.trim)
    lila.mon.relay.push(name = rt.fullName, user = me.username, client = client)(
      moves = results.collect { case Right(a) => a.moves }.sum,
      errors = results.count(_.isLeft)
    )

  private def push(prev: RelayRound.WithTour, rawGames: Vector[RelayGame]) =
    workQueue(prev.round.id):
      for
        rt <- api.byIdWithTour(prev.round.id).orFail(s"Relay $prev no longer available")
        _ <- cantHaveUpstream(rt.round).so(fail => fufail[Unit](fail.error))
        withPlayers <- playerEnrich.enrichAndReportAmbiguous(rt)(rawGames)
        withFide <- fidePlayers.enrichGames(rt.tour)(withPlayers)
        games = rt.tour.teams.fold(withFide)(_.update(withFide))
        event <- sync
          .updateStudyChapters(rt, games)
          .map: res =>
            SyncLog.event(res.nbMoves, none)
          .recover:
            case e: Exception => SyncLog.event(0, e.some)
        _ = if !rt.round.hasStarted && !rt.tour.official && event.hasMoves then
          irc.broadcastStart(rt.round.id, rt.fullName)
        allGamesFinished <- (games.nonEmpty && games.forall(_.points.isDefined)).so:
          chapterPreview.dataList(rt.round.studyId).map(_.forall(_.finished))
        round <- api.update(rt.round): r1 =>
          val r2 = r1.withSync(_.addLog(event))
          val r3 = if event.hasMoves then r2.ensureStarted.resume(rt.tour.official) else r2
          val finishedAt = allGamesFinished.option(r3.finishedAt.|(nowInstant))
          r3.copy(finishedAt = finishedAt)
        _ <- games.nonEmpty.so(api.syncTargetsOfSource(round))
      yield ()

  private def pgnToGames(pgnBody: PgnStr, tc: Option[TournamentClock]): List[Either[Failure, RelayGame]] =
    RelayFetch.injectTimeControl
      .in(tc)(MultiPgn.split(pgnBody, RelayFetch.maxChaptersToShow))
      .value
      .map: pgn =>
        validate(pgn).map: importResult =>
          RelayGame.fromStudyImport(StudyPgnImport.result(importResult, Nil))

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
