package lila.api

import akka.actor._
import java.lang.management.ManagementFactory
import scala.concurrent.duration._

import lila.hub.actorApi.round.NbRounds
import lila.socket.actorApi.NbMembers

private final class KamonPusher(
    countUsers: () => Int) extends Actor {

  import KamonPusher._

  override def preStart() {
    scheduleTick
  }

  private val threadStats = ManagementFactory.getThreadMXBean
  private val app = lila.common.PlayApp

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(1 second, self, Tick)

  def receive = {

    case NbMembers(nb) =>
      lila.mon.socket.member(nb)

    case NbRounds(nb) =>
      lila.mon.round.actor.count(nb)

    case Tick =>
      lila.mon.jvm.thread(threadStats.getThreadCount)
      lila.mon.jvm.daemon(threadStats.getDaemonThreadCount)
      lila.mon.jvm.uptime(app.uptime.toStandardSeconds.getSeconds)
      lila.mon.user.online(countUsers())
      scheduleTick
  }
}

object KamonPusher {

  private case object Tick

  def start(system: ActorSystem)(instance: => Actor) =
    system.lilaBus.subscribe(system.actorOf(Props(instance)), 'nbMembers, 'nbRounds)
}

import com.typesafe.config.Config
import kamon.metric.{ MetricKey, Entity }
import kamon.statsd._

// don't replace . with _
// replace / with .
class KeepDotsMetricKeyGenerator(config: Config) extends SimpleMetricKeyGenerator(config) {

  override def createNormalizer(strategy: String): Normalizer = strategy match {
    case "keep-dots" => (s: String) ⇒ s
      .replace(": ", "-")
      .replace(" ", "_")
      .replace("/", ".")
    case _ => super.createNormalizer(strategy)
  }
}
