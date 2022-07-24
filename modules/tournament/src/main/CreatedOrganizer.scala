package lila.tournament

import akka.stream.scaladsl._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaScheduler, LilaStream }

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(implicit ec: ExecutionContext, scheduler: akka.actor.Scheduler, mat: akka.stream.Materializer) {

  LilaScheduler(_.Every(2 seconds), _.AtMost(20 seconds), _.Delay(18 seconds)) {
    tournamentRepo.shouldStartCursor
      .documentSource()
      .mapAsync(1)(api.start)
      .toMat(Sink.ignore)(Keep.right)
      .run()
      .monSuccess(_.tournament.createdOrganizer.tick)
      .void
  }
}
