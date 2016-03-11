package lila.round

import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot

private final class MoveMonitor(
    system: ActorSystem,
    channel: ActorRef) {

  def record(nanos: Option[Long]) = {
    nanos foreach lila.mon.round.move.time
    lila.mon.round.move.count()
  }

  Kamon.metrics.subscribe("histogram", "round.move", system.actorOf(Props(new Actor {
    def receive = {
      case tick: TickMetricSnapshot => tick.metrics.collectFirst {
        case (entity, snapshot) if entity.category == "histogram" => snapshot
      } flatMap (_ histogram "histogram") foreach { h =>
        if (!h.isEmpty) channel ! lila.socket.Channel.Publish(
          lila.socket.Socket.makeMessage("mlat", (h.sum / h.numberOfMeasurements / 1000000).toInt)
        )
      }
    }
  })))
}
