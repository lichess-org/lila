package lila.relay

import scala.concurrent.duration.*
import akka.actor.*
import chess.format.pgn.{ PgnStr, Parser, Std, San, Tags }
import chess.{ ErrorStr, Game, Replay, Square }
import akka.pattern.after

import lila.study.MultiPgn
import lila.base.LilaInvalid
import lila.hub.AsyncActorSequencers
import lila.common.config.Max

final class RelayPush(sync: RelaySync, api: RelayApi, irc: lila.irc.IrcApi)(using
    ActorSystem,
    Executor,
    Scheduler
):

  private val workQueue = AsyncActorSequencers[RelayRoundId](
    maxSize = Max(8),
    expiration = 1 minute,
    timeout = 10 seconds,
    name = "relay.push"
  )

  case class Failure(tags: Tags, error: String)
  case class Success(tags: Tags, moves: Int)

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[List[Either[Failure, Success]]] =
    if rt.round.sync.hasUpstream
    then fuccess(List(Left(Failure(Tags.empty, "The relay has an upstream URL, and cannot be pushed to."))))
    else
      val parsed = pgnToGames(rt, pgn)
      rt.round.sync.nonEmptyDelay.fold(push(rt, parsed)): delay =>
        after(delay.value.seconds)(push(rt, parsed))
        fuccess(parsed.map(e => e.map(g => Success(g.tags, g.root.mainline.size))))

  private def push(rt: RelayRound.WithTour, parsed: List[Either[Failure, RelayGame]]) =
    workQueue(rt.round.id):
      parsed
        .map:
          case Left(err) => fuccess(Left(err))
          case Right(game) =>
            sync
              .updateStudyChapters(rt, Vector(game))
              .map: res =>
                if !rt.round.hasStarted && !rt.tour.official && res.nbMoves > 0 then
                  irc.broadcastStart(rt.round.id, rt.fullName)
                Right(Success(game.tags, game.root.mainline.size))
              .recover:
                case e: Exception =>
                  Left(Failure(game.tags, e.getMessage))
        .sequence
        .flatMap: results =>
          val events = results.map: e =>
            e match
              case Left(err) => SyncLog.event(0, Exception(err.error).some)
              case Right(s)  => SyncLog.event(s.moves, none)
          api
            .update(rt.round): r1 =>
              val r2 = events.foldLeft(r1)((r, e) => r.withSync(_ addLog e))
              val r3 = if events.exists(_.moves > 0) then r2.ensureStarted.resume else r2
              val finished =
                parsed.forall(_.isRight) && parsed.forall(_.right.exists(_.ending.isDefined))
              r3.copy(finished = finished) // these dangling objects were in your original code
            .inject(results)

  private def pgnToGames(rt: RelayRound.WithTour, pgnBody: PgnStr): List[Either[Failure, RelayGame]] =
    MultiPgn
      .split(pgnBody, Max(128))
      .value
      .map: pgn =>
        validate(pgn) match
          case Left(err) => Left(err)
          case Right(valid) =>
            lila.study.PgnImport(pgn, Nil) match
              case Left(errStr) => Left(Failure(valid.tags, errStr.value))
              case Right(game) =>
                Right:
                  RelayGame(
                    tags = game.tags,
                    variant = game.variant,
                    root = game.root.copy(
                      comments = lila.tree.Node.Comments.empty,
                      children = game.root.children
                        .updateMainline(_.copy(comments = lila.tree.Node.Comments.empty))
                    ),
                    ending = game.end
                  )

  // silently consume DGT board king-check move to center at game end
  private def validate(pgnBody: PgnStr): Either[Failure, Success] =
    Parser
      .full(pgnBody)
      .fold(
        err => Left(Failure(Tags.empty, oneline(err))),
        parsed =>
          val game = Game(variantOption = parsed.tags.variant, fen = parsed.tags.fen)
          val (maybeErr, replay) = parsed.mainline.foldLeft((Option.empty[ErrorStr], Replay(game))):
            case (acc @ (Some(_), _), _) => acc
            case ((none, r), san) =>
              san(r.state.situation).fold(err => (err.some, r), mv => (none, r.addMove(mv)))
          maybeErr.fold(Right(Success(parsed.tags, parsed.mainline.size))): err =>
            parsed.mainline.lastOption match
              case Some(mv: Std) if isFatal(mv, replay, parsed.mainline) =>
                Left(Failure(parsed.tags, oneline(err)))
              case _ => Right(Success(parsed.tags, parsed.mainline.size - 1))
      )

  private def isFatal(mv: Std, replay: Replay, parsed: List[San]) =
    import Square.*
    replay.moves.size < parsed.size - 1
    || mv.role.forsyth != 'k'
    || (mv.dest != D4 && mv.dest != D5 && mv.dest != E4 && mv.dest != E5)

  private def oneline(err: ErrorStr) = err.value.linesIterator.nextOption.getOrElse("error")
