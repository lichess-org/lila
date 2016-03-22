package lila.round

import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot

private final class MoveMonitor(
    system: ActorSystem,
    channel: ActorRef) {

  Kamon.metrics.subscribe("trace", "round.move.trace", system.actorOf(Props(new Actor {
    def receive = {
      case tick: TickMetricSnapshot => tick.metrics.collectFirst {
        case (entity, snapshot) if entity.category == "trace" => snapshot
      } flatMap (_ histogram "elapsed-time") foreach { h =>
        if (!h.isEmpty) channel ! lila.socket.Channel.Publish(
          lila.socket.Socket.makeMessage("mlat", (h.sum / h.numberOfMeasurements / 1000000).toInt)
        )
      }
    }
  })))
}
