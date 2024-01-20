package lila.relay

import scala.concurrent.duration.*
import akka.actor.*
import chess.format.pgn.{ ParsedPgn, PgnStr, Parser, Reader }
import akka.pattern.after

import lila.study.MultiPgn
import lila.base.LilaInvalid

final class RelayPush(sync: RelaySync, api: RelayApi, irc: lila.irc.IrcApi)(using
    ActorSystem,
    Executor,
    ClassicActorSystemProvider
):

  private val throttler = lila.hub.EarlyMultiThrottler[RelayRoundId](logger)

  type Result = Either[LilaInvalid, Int]

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
    if rt.round.sync.hasUpstream
    then fuccess(Left(LilaInvalid("The relay has an upstream URL, and cannot be pushed to.")))
    else
      throttler.ask[Result](rt.round.id, 1.seconds):
        errors(rt, pgn) match
          case Some(err) => fuccess(Left(err))
          case None =>
            rt.round.sync.delay match
              case Some(seconds) =>
                after(seconds.value.seconds)(push(rt, pgn))
                fuccess(Right(0))
              case none => push(rt, pgn)

  private def errors(rt: RelayRound.WithTour, pgn: PgnStr): Option[LilaInvalid] =
    Reader.full(pgn) match
      case Left(error) => LilaInvalid(error.toString).some
      case Right(parsed) =>
        parsed match
          case Reader.Result.Incomplete(_, errorStr) =>
            LilaInvalid(errorStr.toString).some
          case Reader.Result.Complete(_) =>
            RelayFetch
              .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour))) match
              case Left(error) => LilaInvalid(error.toString).some
              case _           => none

  private def push(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
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
