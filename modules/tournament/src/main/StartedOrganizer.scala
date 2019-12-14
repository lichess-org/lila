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

  override def preStart: Unit = {
    context setReceiveTimeout 15.seconds
    scheduleNext
  }

  implicit def ec = context.dispatcher

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(3 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.StartedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      tournamentRepo.startedCursor
        .documentSource()
        .mapAsync(1) { tour =>
          // pairingLogger.info(tour.toString)
          if (tour.secondsToFinish <= 0) api finish tour inject 0
          else {
            def pairIfStillTime = (!tour.pairingsClosed && tour.nbPlayers > 1) ?? startPairing(tour)
            if (!tour.isScheduled && tour.nbPlayers < 40)
              playerRepo nbActiveUserIds tour.id flatMap { nb =>
                if (nb < 2) api finish tour inject 0
                else pairIfStillTime
              } else pairIfStillTime
          }
        }
        .log(getClass.getName)
        .toMat(Sink.fold(0 -> 0) {
          case ((tours, users), tourUsers) => (tours + 1, users + tourUsers)
        })(Keep.right)
        .run
        .addEffect {
          case (tours, users) =>
            lila.mon.tournament.started.update(tours)
            lila.mon.tournament.waitingPlayers.record(users)
        }
        .monSuccess(_.tournament.startedOrganizer.tick)
        .addEffectAnyway(scheduleNext)
  }

  // returns number of users actively awaiting a pairing
  private def startPairing(tour: Tournament): Fu[Int] =
    socket
      .getWaitingUsers(tour)
      .monSuccess(_.tournament.startedOrganizer.waitingUsers)
      .flatMap { waiting =>
        api.makePairings(tour, waiting) inject waiting.size
      }
}
