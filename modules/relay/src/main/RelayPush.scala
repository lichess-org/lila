package lila.relay

import akka.actor.*
import akka.pattern.after
import chess.format.pgn.{ Parser, PgnStr, San, Std, Tags }
import chess.{ ErrorStr, Game, Replay, Square, TournamentClock }
import scalalib.actor.AsyncActorSequencers

import scala.concurrent.duration.*

import lila.study.{ ChapterPreviewApi, MultiPgn, StudyPgnImport }

final class RelayPush(
    sync: RelaySync,
    api: RelayApi,
    chapterPreview: ChapterPreviewApi,
    fidePlayers: RelayFidePlayerApi,
    playerEnrich: RelayPlayerEnrich,
    irc: lila.core.irc.IrcApi
)(using ActorSystem, Executor, Scheduler):

  private val workQueue = AsyncActorSequencers[RelayRoundId](
    maxSize = Max(32),
    expiration = 1.minute,
    timeout = 10.seconds,
    name = "relay.push",
    lila.log.asyncActorMonitor.full
  )

  case class Failure(tags: Tags, error: String)
  case class Success(tags: Tags, moves: Int)

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[List[Either[Failure, Success]]] =
    if rt.round.sync.hasUpstream
    then fuccess(List(Left(Failure(Tags.empty, "The relay has an upstream URL, and cannot be pushed to."))))
    else
      val parsed = pgnToGames(pgn, rt.tour.info.clock)
      val games  = parsed.collect { case Right(g) => g }.toVector
      val response: List[Either[Failure, Success]] =
        parsed.map(_.map(g => Success(g.tags, g.root.mainline.size)))
      val andSyncTargets = response.exists(_.isRight)

      rt.round.sync.delayMinusLag
        .ifTrue(games.exists(_.root.children.nonEmpty))
        .match
          case None => push(rt, games, andSyncTargets).inject(response)
          case Some(delay) =>
            after(delay.value.seconds)(push(rt, games, andSyncTargets))
            fuccess(response)

  private def push(rt: RelayRound.WithTour, rawGames: Vector[RelayGame], andSyncTargets: Boolean) =
    workQueue(rt.round.id):
      for
        withPlayers <- playerEnrich.enrichAndReportAmbiguous(rt)(rawGames)
        withFide    <- fidePlayers.enrichGames(rt.tour)(withPlayers)
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
          val r2         = r1.withSync(_.addLog(event))
          val r3         = if event.hasMoves then r2.ensureStarted.resume(rt.tour.official) else r2
          val finishedAt = allGamesFinished.option(r3.finishedAt.|(nowInstant))
          r3.copy(finishedAt = finishedAt)
        _ <- andSyncTargets.so(api.syncTargetsOfSource(round))
      yield ()

  private def pgnToGames(pgnBody: PgnStr, tc: Option[TournamentClock]): List[Either[Failure, RelayGame]] =
    RelayFetch.injectTimeControl
      .in(tc)(MultiPgn.split(pgnBody, RelayFetch.maxChaptersToShow))
      .value
      .map: pgn =>
        validate(pgn).flatMap: tags =>
          StudyPgnImport
            .result(pgn, Nil)
            .fold(
              errStr => Left(Failure(tags, oneline(errStr))),
              game => Right(RelayGame.fromStudyImport(game))
            )

  // silently consume DGT board king-check move to center at game end
  private def validate(pgnBody: PgnStr): Either[Failure, Tags] =
    Parser
      .full(pgnBody)
      .fold(
        err => Left(Failure(Tags.empty, oneline(err))),
        parsed =>
          val game = Game(variantOption = parsed.tags.variant, fen = parsed.tags.fen)

          val (maybeErr, replay) = parsed.mainline.foldLeft((none[ErrorStr], Replay(game))):
            case (acc @ (Some(_), _), _) => acc
            case ((none, r), san) =>
              san(r.state.situation).fold(err => (err.some, r), mv => (none, r.addMove(mv)))

          maybeErr.fold(Right(parsed.tags)): err =>
            parsed.mainline.lastOption match
              case Some(mv: Std) if isFatal(mv, replay, parsed.mainline) =>
                Left(Failure(parsed.tags, oneline(err)))
              case _ => Right(parsed.tags)
      )

  private def isFatal(mv: Std, replay: Replay, parsed: List[San]) =
    import Square.*
    replay.moves.size < parsed.size - 1
    || mv.role != chess.King
    || (mv.dest != D4 && mv.dest != D5 && mv.dest != E4 && mv.dest != E5)

  private def oneline(err: ErrorStr) = err.value.linesIterator.nextOption.getOrElse("error")
