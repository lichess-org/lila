package lila.common

import scala.concurrent.duration._

import akka.actor._

object ResilientScheduler {

  private case object Tick

  def apply(
    every: FiniteDuration,
    atMost: FiniteDuration,
    system: ActorSystem,
    logger: lila.log.Logger
  )(f: => Funit): Unit =

    system.actorOf(Props(new Actor {

      override def preStart: Unit = {
        context setReceiveTimeout (atMost + 2.second)
        scheduleNext
      }

      def scheduleNext = context.system.scheduler.scheduleOnce(every, self, Tick)

      def receive = {

        case ReceiveTimeout =>
          val msg = s"ResilientScheduler timed out after $atMost"
          logger error msg
          throw new RuntimeException(msg)

        case Tick => f >>- scheduleNext
      }
    }))
}
