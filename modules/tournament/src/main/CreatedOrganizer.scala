package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

import lila.common.LilaStream

private final class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(implicit mat: akka.stream.Materializer) extends Actor {

  override def preStart: Unit = {
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

    case Tick => tournamentRepo
      .allCreatedCursor(30)
      .documentSource()
      .mapAsync(1) { tour =>
        tour.schedule match {
          // #TODO better tournament API sequencer returning Futures (future queue?)
          case None if tour.isPrivate && tour.hasWaitedEnough => fuccess {
            api start tour
          }
          case None => playerRepo count tour.id flatMap {
            case 0 => api wipe tour
            case nb if tour.hasWaitedEnough =>
              if (nb >= Tournament.minPlayers) fuccess {
                api start tour
              }
              else api wipe tour
            case _ => funit
          }
          case Some(_) if tour.hasWaitedEnough => fuccess {
            api start tour
          }
        }
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run
      .addEffect(lila.mon.tournament.created(_))
      .chronometer
      .mon(_.tournament.createdOrganizer.tickTime)
      .logIfSlow(500, logger)(_ => "CreatedOrganizer.Tick")
      .result addEffectAnyway scheduleNext
  }
}
