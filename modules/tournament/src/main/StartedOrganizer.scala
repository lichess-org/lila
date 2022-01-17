package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import lila.common.LilaStream

final private class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    socket: TournamentSocket
)(implicit mat: akka.stream.Materializer)
    extends Actor {

  implicit def ec = context.dispatcher

  override def preStart(): Unit = {
    context setReceiveTimeout 60.seconds
    context.system.scheduler.scheduleOnce(25 seconds, self, Tick(0)).unit
  }

  case class Tick(it: Int)

  def scheduleNext(prev: Int): Unit =
    context.system.scheduler.scheduleOnce(1 second, self, Tick(prev + 1)).unit

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.StartedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick(tickIt) =>
      val doAllTournaments = tickIt % 20 == 0
      tournamentRepo
        .startedCursorWithNbPlayersGte {
          if (doAllTournaments) none // every 20s, do all tournaments
          else if (tickIt % 2 == 0) 50.some // every 2s, do all decent tournaments
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
        }
        .monSuccess(_.tournament.startedOrganizer.tick)
        .addEffectAnyway(scheduleNext(tickIt))
        .unit
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
