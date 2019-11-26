package lila.api

import akka.actor._
import java.lang.management.ManagementFactory
import scala.concurrent.duration._

private final class KamonPusher extends Actor {

  import KamonPusher._

  override def preStart(): Unit = {
    scheduleTick
  }

  private val threadStats = ManagementFactory.getThreadMXBean
  private val app = lila.common.PlayApp

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(1 second, self, Tick)

  def receive = {

    case Tick =>
      lila.mon.jvm.thread(threadStats.getThreadCount)
      lila.mon.jvm.daemon(threadStats.getDaemonThreadCount)
      lila.mon.jvm.uptime(app.uptimeSeconds)
      scheduleTick
  }
}

object KamonPusher {

  private case object Tick

  def start(system: ActorSystem) =
    lila.common.Bus.subscribe(system.actorOf(Props(new KamonPusher)))
}
