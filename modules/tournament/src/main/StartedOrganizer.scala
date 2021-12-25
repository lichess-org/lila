package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

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
      tournamentRepo
        .startedCursor {
          if (tickIt % 20 == 0) 2 // every 20s, do all tournaments with 2+ players
          else if (tickIt % 2 == 0) 50 // every 2s, do all decent tournaments
          else 1000 // always do massive tournaments
        }
        .documentSource()
        .mapAsyncUnordered(4) { tour =>
          processTour(tour) recover { case e: Exception =>
            logger.error(s"StartedOrganizer $tour", e)
            0
          }
        }
        .toMat(Sink.fold(0 -> 0) { case ((tours, users), tourUsers) =>
          (tours + 1, users + tourUsers)
        })(Keep.right)
        .run()
        .addEffect { case (tours, users) =>
          lila.mon.tournament.started.update(tours)
          lila.mon.tournament.waitingPlayers.record(users).unit
        }
        .monSuccess(_.tournament.startedOrganizer.tick)
        .addEffectAnyway(scheduleNext(tickIt))
        .unit
  }

  private def processTour(tour: Tournament): Fu[Int] =
    if (tour.secondsToFinish <= 0) api finish tour inject 0
    else if (api.killSchedule contains tour.id) {
      api.killSchedule remove tour.id
      api finish tour inject 0
    } else if (tour.nbPlayers < 30) {
      playerRepo nbActiveUserIds tour.id flatMap { nb =>
        (nb >= 2) ?? startPairing(tour)
      }
    } else startPairing(tour)

  // returns number of users actively awaiting a pairing
  private def startPairing(tour: Tournament): Fu[Int] =
    (!tour.pairingsClosed && tour.nbPlayers > 1) ??
      socket
        .getWaitingUsers(tour)
        .monSuccess(_.tournament.startedOrganizer.waitingUsers)
        .flatMap { waiting =>
          api.makePairings(tour, waiting) inject waiting.size
        }
}
