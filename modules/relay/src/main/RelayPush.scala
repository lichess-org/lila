package lila.relay

import akka.actor.*
import chess.format.pgn.PgnStr

import lila.study.MultiPgn

final class RelayPush(sync: RelaySync, api: RelayApi)(using ActorSystem, Executor):

  private val throttler = lila.hub.EarlyMultiThrottler[RelayRoundId](logger)

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Option[String]] =
    if rt.round.sync.hasUpstream
    then fuccess("The relay has an upstream URL, and cannot be pushed to.".some)
    else
      fuccess:
        throttler(rt.round.id, if rt.tour.official then 3.seconds else 7.seconds):
          pushNow(rt, pgn)
        none

  private def pushNow(rt: RelayRound.WithTour, pgn: PgnStr): Funit =
    RelayFetch
      .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour)))
      .toFuture
      .flatMap: games =>
        sync(rt, games)
          .map: res =>
            SyncLog.event(res.nbMoves, none)
          .recover:
            case e: Exception => SyncLog.event(0, e.some)
          .flatMap: event =>
            api
              .update(rt.round):
                _.withSync(_ addLog event).copy(finished = games.forall(_.end.isDefined))
              .void
