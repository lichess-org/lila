package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

final private class CreatedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo
)(implicit mat: akka.stream.Materializer)
    extends Actor {

  implicit def ec = context.dispatcher

  override def preStart(): Unit = {
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(18 seconds, self, Tick).unit
  }

  case object Tick

  def scheduleNext(): Unit =
    context.system.scheduler.scheduleOnce(2 seconds, self, Tick).unit

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.CreatedOrganizer timed out!"
      pairingLogger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      tournamentRepo.shouldStartCursor
        .documentSource()
        .mapAsync(1)(api.start)
        .log(getClass.getName)
        .toMat(Sink.ignore)(Keep.right)
        .run()
        .monSuccess(_.tournament.createdOrganizer.tick)
        .addEffectAnyway(scheduleNext())
        .unit
  }
}
