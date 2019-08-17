package lila.common

import scala.concurrent.duration._

import akka.actor._

object ResilientScheduler {

  private case object Tick
  private case object Done

  def apply(
    every: FiniteDuration,
    atMost: FiniteDuration,
    system: ActorSystem,
    logger: lila.log.Logger
  )(f: => Funit): Unit =

    system.actorOf(Props(new Actor {

      override def preStart: Unit = scheduleNext

      def scheduleNext = context.system.scheduler.scheduleOnce(every, self, Tick)

      def receive = {

        case ReceiveTimeout =>
          val msg = s"ResilientScheduler timed out after $atMost"
          logger error msg
          throw new RuntimeException(msg)

        case Tick =>
          context setReceiveTimeout (atMost + 2.second)
          f >>- { self ! Done }

        case Done =>
          if (every > atMost) context.setReceiveTimeout(Duration.Undefined)
          scheduleNext
      }
    }))
}
