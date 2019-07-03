package lila.round

import scala.concurrent.duration._
import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot

private object MoveMonitor {

  def start(system: ActorSystem, channel: lila.socket.Channel) =

    Kamon.metrics.subscribe("trace", "round.move.trace", system.actorOf(Props(new Actor {
      var current: Int = 0
      context.system.scheduler.schedule(5 second, 2 second) {
        channel ! lila.socket.Channel.Publish(lila.socket.Socket.makeMessage("mlat", current))
        system.lilaBus.publish(lila.hub.actorApi.round.Mlat(current), 'mlat)
      }
      def receive = {
        case tick: TickMetricSnapshot => tick.metrics.collectFirst {
          case (entity, snapshot) if entity.category == "trace" => snapshot
        } flatMap (_ histogram "elapsed-time") foreach { h =>
          if (!h.isEmpty) current = (h.sum / h.numberOfMeasurements / 1000000).toInt
        }
      }
    })))
}
