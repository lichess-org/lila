package lila.tournament

import akka.actor._
import scala.concurrent.duration._

private final class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    socket: TournamentSocket
) extends Actor {

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

    case Tick =>
      val startAt = nowMillis
      tournamentRepo.startedTours.flatMap { started =>
        lila.common.Future.traverseSequentially(started) { tour =>
          if (tour.secondsToFinish <= 0) fuccess(api finish tour)
          else {
            def pairIfStillTime = (!tour.pairingsClosed && tour.nbPlayers > 1) ?? startPairing(tour, startAt)
            if (!tour.isScheduled && tour.nbPlayers < 40)
              playerRepo nbActiveUserIds tour.id flatMap { nb =>
                if (nb < 2) fuccess(api finish tour)
                else pairIfStillTime
              }
            else pairIfStillTime
          }
        }.addEffect { _ =>
          // lila.mon.tournament.player(playerCounts.sum)
          lila.mon.tournament.started(started.size)
        }
      }.chronometer
        .mon(_.tournament.startedOrganizer.tickTime)
        .logIfSlow(500, logger)(_ => "StartedOrganizer.Tick")
        .result addEffectAnyway scheduleNext
  }

  private def startPairing(tour: Tournament, startAt: Long): Funit =
    socket.getWaitingUsers(tour).mon(_.tournament.startedOrganizer.waitingUsersTime) map {
      api.makePairings(tour, _, startAt)
    }
}
