package lila.tournament

import akka.stream.scaladsl.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.common.LilaScheduler

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(using ExecutionContext, akka.actor.Scheduler, akka.stream.Materializer):

  LilaScheduler(
    "Tournament.CreatedOrganizer",
    _.Every(2 seconds),
    _.AtMost(20 seconds),
    _.Delay(18 seconds)
  ) {
    tournamentRepo.shouldStartCursor
      .documentSource()
      .mapAsync(1)(api.start)
      .toMat(Sink.ignore)(Keep.right)
      .run()
      .monSuccess(_.tournament.createdOrganizer.tick)
      .void
  }
