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
      val parsed   = pgnToGames(rt, pgn)
      val games    = parsed.collect { case Right(g) => g }.toVector
      val response = parsed.map(_.map(g => Success(g.tags, g.root.mainline.size)))

      rt.round.sync.nonEmptyDelay.fold(push(rt, games).inject(response)): delay =>
        after(delay.value.seconds)(push(rt, games))
        fuccess(response)

  private def push(rt: RelayRound.WithTour, games: Vector[RelayGame]) =
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

  private def pgnToGames(rt: RelayRound.WithTour, pgnBody: PgnStr): List[Either[Failure, RelayGame]] =
    MultiPgn
      .split(pgnBody, Max(lila.study.Study.maxChapters * (if rt.tour.official then 2 else 1)))
      .value
      .map: pgn =>
        validate(pgn).flatMap: tags =>
          lila.study.PgnImport(pgn, Nil) match
            case Left(errStr) => Left(Failure(tags, oneline(errStr)))
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
  private def validate(pgnBody: PgnStr): Either[Failure, Tags] =
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

          maybeErr.fold(Right(parsed.tags)): err =>
            parsed.mainline.lastOption match
              case Some(mv: Std) if isFatal(mv, replay, parsed.mainline) =>
                Left(Failure(parsed.tags, oneline(err)))
              case _ => Right(parsed.tags)
      )

  private def isFatal(mv: Std, replay: Replay, parsed: List[San]) =
    import Square.*
    replay.moves.size < parsed.size - 1
    || mv.role.forsyth != 'k'
    || (mv.dest != D4 && mv.dest != D5 && mv.dest != E4 && mv.dest != E5)

  private def oneline(err: ErrorStr) = err.value.linesIterator.nextOption.getOrElse("error")
