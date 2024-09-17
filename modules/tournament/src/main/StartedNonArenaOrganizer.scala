package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._

final private class StartedNonArenaOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo
)(implicit mat: akka.stream.Materializer)
    extends Actor {

  override def preStart(): Unit = {
    context setReceiveTimeout 15.seconds
    context.system.scheduler.scheduleOnce(5 seconds, self, Tick).unit
  }

  implicit def ec = context.dispatcher

  case object Tick

  def scheduleNext(): Unit =
    context.system.scheduler.scheduleOnce(5 seconds, self, Tick).unit

  def receive = {

    case ReceiveTimeout =>
      val msg = "tournament.StartedNonArenaOrganizer timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      tournamentRepo.shouldEndNonArenaCursor
        .documentSource()
        .mapAsync(1)(api.finish)
        .log(getClass.getName)
        .toMat(Sink.ignore)(Keep.right)
        .run()
        .monSuccess(_.tournament.createdOrganizer.tick)
        .addEffectAnyway(scheduleNext())
        .unit
  }

}
