package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

import lila.common.LilaStream

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer)
    extends Actor {

  override def preStart: Unit = {
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
      tournamentRepo
        .startingSoonCursor(30)
        .documentSource()
        .mapAsync(1) { tour =>
          tour.schedule match {
            case None if tour.isPrivate && tour.hasWaitedEnough => api start tour
            case None =>
              playerRepo count tour.id flatMap {
                case 0 => api destroy tour
                case nb if tour.hasWaitedEnough =>
                  if (nb >= Tournament.minPlayers) api start tour
                  else api destroy tour
                case _ => funit
              }
            case Some(_) if tour.hasWaitedEnough => api start tour
            case Some(_)                         => funit
          }
        }
        .log(getClass.getName)
        .toMat(LilaStream.sinkCount)(Keep.right)
        .run
        .addEffect(lila.mon.tournament.created.update(_))
        .monSuccess(_.tournament.createdOrganizer.tick)
        .addEffectAnyway(scheduleNext)
  }
}
