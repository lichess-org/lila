package lila.tournament

import akka.stream.scaladsl._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaScheduler, LilaStream }

final private class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    socket: TournamentSocket
)(implicit ec: ExecutionContext, scheduler: akka.actor.Scheduler, mat: akka.stream.Materializer) {

  var runCounter = 0

  LilaScheduler(_.Every(1 seconds), _.AtMost(30 seconds), _.Delay(26 seconds)) {

    val doAllTournaments = runCounter % 20 == 0

    tournamentRepo
      .startedCursorWithNbPlayersGte {
        if (doAllTournaments) none // every 20s, do all tournaments
        else if (runCounter % 2 == 0) 50.some // every 2s, do all decent tournaments
        else 1000.some // always do massive tournaments
      }
      .documentSource()
      .mapAsyncUnordered(4) { tour =>
        processTour(tour) recover { case e: Exception =>
          logger.error(s"StartedOrganizer $tour", e)
          0
        }
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect { nb =>
        if (doAllTournaments) lila.mon.tournament.started.update(nb).unit
        runCounter = runCounter + 1
      }
      .monSuccess(_.tournament.startedOrganizer.tick)
      .void
  }

  private def processTour(tour: Tournament): Funit =
    if (tour.secondsToFinish <= 0) api finish tour
    else if (api.killSchedule contains tour.id) {
      api.killSchedule remove tour.id
      api finish tour
    } else if (tour.nbPlayers < 2) funit
    else if (tour.nbPlayers < 30) {
      playerRepo nbActiveUserIds tour.id flatMap { nb =>
        (nb >= 2) ?? startPairing(tour)
      }
    } else startPairing(tour)

  private def startPairing(tour: Tournament): Funit =
    (!tour.pairingsClosed && tour.nbPlayers > 1) ??
      socket
        .getWaitingUsers(tour)
        .monSuccess(_.tournament.startedOrganizer.waitingUsers)
        .flatMap { waiting =>
          lila.mon.tournament.waitingPlayers(tour.id).record(waiting.size).unit
          api.makePairings(tour, waiting)
        }
}
