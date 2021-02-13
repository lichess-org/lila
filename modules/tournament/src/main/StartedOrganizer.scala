package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import lila.common.ThreadLocalRandom

final private class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    socket: TournamentSocket
)(implicit mat: akka.stream.Materializer)
    extends Actor {

  override def preStart(): Unit = {
    context setReceiveTimeout 120.seconds
    scheduleNext
  }

  implicit def ec = context.dispatcher

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(2 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.StartedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      tournamentRepo.startedCursor
        .documentSource()
        .mapAsyncUnordered(4) { tour =>
          processTour(tour) recover {
            case e: Exception =>
              logger.error(s"StartedOrganizer $tour", e)
              0
          }
        }
        .toMat(Sink.fold(0 -> 0) {
          case ((tours, users), tourUsers) => (tours + 1, users + tourUsers)
        })(Keep.right)
        .run()
        .addEffect {
          case (tours, users) =>
            lila.mon.tournament.started.update(tours)
            lila.mon.tournament.waitingPlayers.record(users)
        }
        .monSuccess(_.tournament.startedOrganizer.tick)
        .addEffectAnyway(scheduleNext)
  }

  private def processTour(tour: Tournament): Fu[Int] =
    if (tour.secondsToFinish <= 0) api finish tour inject 0
    else if (!tour.isScheduled && tour.nbPlayers < 30 && ThreadLocalRandom.nextInt(10) == 0) {
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
