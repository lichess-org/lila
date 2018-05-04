package lila.tournament

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._

import actorApi._
import lila.hub.actorApi.map.Ask
import makeTimeout.short

private final class StartedOrganizer(
    api: TournamentApi,
    reminder: ActorRef,
    isOnline: String => Boolean,
    socketHub: ActorRef
) extends Actor {

  override def preStart: Unit = {
    pairingLogger.info("Start StartedOrganizer")
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
      TournamentRepo.startedTours.flatMap { started =>
        lila.common.Future.traverseSequentially(started) { tour =>
          PlayerRepo activeUserIds tour.id flatMap { activeUserIds =>
            val nb = activeUserIds.size
            val result: Funit =
              if (tour.secondsToFinish <= 0) fuccess(api finish tour)
              else if (!tour.isScheduled && nb < 2) fuccess(api finish tour)
              else if (!tour.pairingsClosed) startPairing(tour, activeUserIds, startAt)
              else funit
            result >>- {
              reminder ! RemindTournament(tour, activeUserIds)
            } inject nb
          }
        }.addEffect { playerCounts =>
          lila.mon.tournament.player(playerCounts.sum)
          lila.mon.tournament.started(started.size)
        }
      }.chronometer
        .mon(_.tournament.startedOrganizer.tickTime)
        .logIfSlow(500, logger)(_ => "StartedOrganizer.Tick")
        .result addEffectAnyway scheduleNext
  }

  private def startPairing(tour: Tournament, activeUserIds: List[String], startAt: Long): Funit =
    getWaitingUsers(tour) zip PairingRepo.playingUserIds(tour) map {
      case (waitingUsers, playingUserIds) =>
        val users = waitingUsers intersect activeUserIds.toSet diff playingUserIds
        api.makePairings(tour, users, startAt)
    }

  private def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] =
    socketHub ? Ask(tour.id, GetWaitingUsers) mapTo manifest[WaitingUsers]
}
