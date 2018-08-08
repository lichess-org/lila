package lidraughts.api

import akka.actor._
import java.lang.management.ManagementFactory
import scala.concurrent.duration._

import lidraughts.hub.actorApi.round.NbRounds
import lidraughts.socket.actorApi.NbMembers

private final class KamonPusher(
    countUsers: () => Int
) extends Actor {

  import KamonPusher._

  override def preStart(): Unit = {
    scheduleTick
  }

  private val threadStats = ManagementFactory.getThreadMXBean
  private val app = lidraughts.common.PlayApp

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(1 second, self, Tick)

  def receive = {

    case NbMembers(nb) =>
      lidraughts.mon.socket.member(nb)

    case NbRounds(nb) =>
      lidraughts.mon.round.actor.count(nb)

    case Tick =>
      lidraughts.mon.jvm.thread(threadStats.getThreadCount)
      lidraughts.mon.jvm.daemon(threadStats.getDaemonThreadCount)
      lidraughts.mon.jvm.uptime(app.uptime.toStandardSeconds.getSeconds)
      lidraughts.mon.user.online(countUsers())
      scheduleTick
  }
}

object KamonPusher {

  private case object Tick

  def start(system: ActorSystem)(instance: => Actor) =
    system.lidraughtsBus.subscribe(system.actorOf(Props(instance)), 'nbMembers, 'nbRounds)
}
