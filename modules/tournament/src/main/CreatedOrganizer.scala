package lila.tournament

import akka.actor._
import scala.concurrent.duration._

private final class CreatedOrganizer(
    api: TournamentApi,
    isOnline: String => Boolean
) extends Actor {

  override def preStart: Unit = {
    pairingLogger.info("Start CreatedOrganizer")
    context setReceiveTimeout 15.seconds
    scheduleNext
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(2 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.CreatedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      TournamentRepo.allCreated(30).map { tours =>
        tours foreach { tour =>
          tour.schedule match {
            case None if tour.isPrivate && tour.hasWaitedEnough => api start tour
            case None => PlayerRepo count tour.id foreach {
              case 0 => api wipe tour
              case nb if tour.hasWaitedEnough =>
                if (nb >= Tournament.minPlayers) api start tour
                else api wipe tour
              case _ =>
            }
            case Some(schedule) if tour.hasWaitedEnough => api start tour
            case _ => funit
          }
        }
        lila.mon.tournament.created(tours.size)
      }.chronometer
        .mon(_.tournament.createdOrganizer.tickTime)
        .logIfSlow(500, logger)(_ => "CreatedOrganizer.Tick")
        .result addEffectAnyway scheduleNext
  }
}
