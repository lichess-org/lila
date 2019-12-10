package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

import lila.common.LilaStream

private final class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    socket: TournamentSocket
)(implicit mat: akka.stream.Materializer) extends Actor {

  override def preStart: Unit = {
    context setReceiveTimeout 15.seconds
    scheduleNext
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(3 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.StartedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick => tournamentRepo.startedCursor
      .documentSource()
      .mapAsync(1) { tour =>
        if (tour.secondsToFinish <= 0) api finish tour
        else {
          def pairIfStillTime = (!tour.pairingsClosed && tour.nbPlayers > 1) ?? startPairing(tour)
          if (!tour.isScheduled && tour.nbPlayers < 40)
            playerRepo nbActiveUserIds tour.id flatMap { nb =>
              if (nb < 2) api finish tour
              else pairIfStillTime
            }
          else pairIfStillTime
        }
      }
      .log(getClass.getName)
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run
      .addEffect(lila.mon.tournament.started(_))
      .chronometer
      .mon(_.tournament.startedOrganizer.tickTime)
      .logIfSlow(500, logger)(nb => s"Pairings for $nb tournaments")
      .result addEffectAnyway scheduleNext
  }

  private def startPairing(tour: Tournament): Funit =
    socket.getWaitingUsers(tour)
      .mon(_.tournament.startedOrganizer.waitingUsersTime)
      .flatMap { api.makePairings(tour, _) }
}
