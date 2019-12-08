package lila.api

import akka.actor._
import java.lang.management.ManagementFactory
import scala.concurrent.duration._

import lila.common.Uptime

private final class KamonPusher extends Actor {

  import KamonPusher._

  override def preStart(): Unit = {
    scheduleTick
  }

  private val threadStats = ManagementFactory.getThreadMXBean

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(1 second, self, Tick)

  def receive = {

    case Tick =>
      lila.mon.bus.classifiers(lila.common.Bus.size)
      scheduleTick
  }
}

object KamonPusher {

  private case object Tick

  def start(system: ActorSystem) =
    lila.common.Bus.subscribe(system.actorOf(Props(new KamonPusher)))
}
