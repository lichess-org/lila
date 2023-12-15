package lila.relay

import akka.actor.*
import chess.format.pgn.PgnStr

import lila.study.MultiPgn
import lila.base.LilaInvalid

final class RelayPush(sync: RelaySync, api: RelayApi)(using ActorSystem, Executor):

  private val throttler = lila.hub.EarlyMultiThrottler[RelayRoundId](logger)

  type Result = Either[LilaInvalid, String]

  def apply(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
    if rt.round.sync.hasUpstream
    then fuccess(Left(LilaInvalid("The relay has an upstream URL, and cannot be pushed to.")))
    else
      throttler.ask[Result](rt.round.id, if rt.tour.official then 3.seconds else 7.seconds):
        pushNow(rt, pgn)

  private def pushNow(rt: RelayRound.WithTour, pgn: PgnStr): Fu[Result] =
    RelayFetch
      .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour)))
      .fold(
        err => fuccess(Left(err)),
        games =>
          sync(rt, games)
            .map: res =>
              SyncLog.event(res.nbMoves, none)
            .recover:
              case e: Exception => SyncLog.event(0, e.some)
            .flatMap: event =>
              api
                .update(rt.round):
                  _.withSync(_ addLog event).copy(finished = games.forall(_.end.isDefined))
                .inject:
                  event.error.fold(Right(s"${event.moves} new moves"))(err => Left(LilaInvalid(err)))
      )
