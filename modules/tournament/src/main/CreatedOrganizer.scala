package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) extends Actor {

  override def preStart(): Unit = {
    context setReceiveTimeout 15.seconds
    context.system.scheduler.scheduleOnce(10 seconds, self, Tick)
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
      tournamentRepo.shouldStartCursor
        .documentSource()
        .mapAsync(1) { tour =>
          playerRepo count tour.id flatMap {
            case 0 => api destroy tour
            case _ => api start tour
          }
        }
        .log(getClass.getName)
        .toMat(Sink.ignore)(Keep.right)
        .run()
        .monSuccess(_.tournament.createdOrganizer.tick)
        .addEffectAnyway(scheduleNext)
  }
}
