package lila.relay

import scala.concurrent.duration.*
import akka.actor.*
import chess.format.pgn.{ PgnStr, Reader }
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
      rt.round.sync.nonEmptyDelay.fold(push(rt, pgn)): delay =>
        validateForDelay(rt, pgn) match
          case Some(err) => fuccess(Left(err))
          case None      => after(delay.value.seconds)(push(rt, pgn))

  private def validateForDelay(rt: RelayRound.WithTour, pgn: PgnStr): Option[LilaInvalid] =
    Reader
      .full(pgn)
      .fold(
        err => LilaInvalid(err.value).some,
        _ match
          case Reader.Result.Incomplete(_, errorStr) => LilaInvalid(errorStr.value).some
          case Reader.Result.Complete(_) =>
            RelayFetch
              .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour)))
              .left
              .toOption
      )

  private def push(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
    workQueue(rt.round.id):
      RelayFetch
        .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour)))
        .fold(
          err => fuccess(Left(err)),
          games =>
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
        )
