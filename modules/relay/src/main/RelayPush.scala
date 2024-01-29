package lila.relay

import scala.concurrent.duration.*
import akka.actor.*
import chess.format.pgn.{ PgnStr, Parser, Std, San }
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

  type Result = Either[LilaInvalid, Int]

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
    if rt.round.sync.hasUpstream
    then fuccess(Left(LilaInvalid("The relay has an upstream URL, and cannot be pushed to.")))
    else
      fatalError(pgn) match
        case Some(fatalErr) => fuccess(Left(fatalErr))
        case _ =>
          pgnToGames(rt, pgn) match
            case Left(err) => fuccess(Left(err))
            case Right(games) =>
              rt.round.sync.nonEmptyDelay.fold(push(rt, games)): delay =>
                after(delay.value.seconds)(push(rt, games))
                fuccess(Right(0))

  private def push(rt: RelayRound.WithTour, games: Vector[RelayGame]): Fu[Result] =
    workQueue(rt.round.id):
      sync
        .updateStudyChapters(rt, games)
        .map: res =>
          SyncLog.event(res.nbMoves, none)
        .recover:
          case e: Exception => SyncLog.event(0, e.some)
        .flatMap: event =>
          if !rt.round.hasStarted && !rt.tour.official && event.hasMoves then
            irc.broadcastStart(rt.round.id, rt.fullName)
          api
            .update(rt.round): r1 =>
              val r2 = r1.withSync(_ addLog event)
              val r3 = if event.hasMoves then r2.ensureStarted.resume else r2
              r3.copy(finished = games.nonEmpty && games.forall(_.ending.isDefined))
            .inject:
              event.error.fold(Right(event.moves))(err => Left(LilaInvalid(err)))

  private def pgnToGames(rt: RelayRound.WithTour, pgnBody: PgnStr): Either[LilaInvalid, Vector[RelayGame]] =
    MultiPgn
      .split(pgnBody, Max(128))
      .value
      .foldLeft[Either[LilaInvalid, Vector[RelayGame]]](Right(Vector.empty)):
        case (left @ Left(_), _) => left
        case (Right(vec), pgn) =>
          lila.study.PgnImport(pgn, Nil) match
            case Left(err) => Left(LilaInvalid(err.value))
            case Right(res) =>
              Right:
                vec :+ RelayGame(
                  tags = res.tags,
                  variant = res.variant,
                  root = res.root.copy(
                    comments = lila.tree.Node.Comments.empty,
                    children = res.root.children
                      .updateMainline(_.copy(comments = lila.tree.Node.Comments.empty))
                  ),
                  ending = res.end
                )

  // silently consume DGT board king-check move to center at game end
  private def fatalError(pgnBody: PgnStr): Option[LilaInvalid] =
    Parser
      .full(pgnBody)
      .fold(
        err => LilaInvalid(err.value).some,
        parsed =>
          val game = Game(variantOption = parsed.tags.variant, fen = parsed.tags.fen)
          val (maybeErr, replay) = parsed.mainline.foldLeft((Option.empty[ErrorStr], Replay(game))):
            case (acc @ (Some(_), _), _) => acc
            case ((none, r), san) =>
              san(r.state.situation).fold(err => (err.some, r), mv => (none, r.addMove(mv)))
          maybeErr.flatMap: err =>
            parsed.mainline.lastOption collect:
              case mv: Std if isFatal(mv, replay, parsed.mainline) => LilaInvalid(err.value)
      )

  private def isFatal(mv: Std, replay: Replay, parsed: List[San]) =
    import Square.*
    replay.moves.size < parsed.size - 1
    || mv.role.forsyth != 'k'
    || (mv.dest != D4 && mv.dest != D5 && mv.dest != E4 && mv.dest != E5)
