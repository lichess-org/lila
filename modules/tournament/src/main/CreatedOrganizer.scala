package lila.tournament

import lila.common.LilaScheduler
import lila.mon.extensions.*

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo
)(using Executor, Scheduler, akka.stream.Materializer):

  LilaScheduler(
    "Tournament.CreatedOrganizer",
    _.Every(2.seconds),
    _.AtMost(20.seconds),
    _.Delay(18.seconds)
  ):
    tournamentRepo.shouldStartCursor
      .documentSource()
      .mapAsync(1)(api.start)
      .run()
      .monSuccess(lila.mon.tournament.createdOrganizer.tick)
      .void
