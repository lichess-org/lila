package lidraughts.tournament

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
      TournamentRepo.shouldStartCursor.map { tours =>
        tours foreach { tour =>
          PlayerRepo count tour.id foreach {
            case 0 if !tour.isPrivate && !tour.isScheduled => api wipe tour
            case _ => api start tour
          }
        }
        lidraughts.mon.tournament.created(tours.size)
      }.chronometer
        .mon(_.tournament.createdOrganizer.tickTime)
        .logIfSlow(500, logger)(_ => "CreatedOrganizer.Tick")
        .result addEffectAnyway scheduleNext
  }
}
