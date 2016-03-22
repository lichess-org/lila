package lila.round

import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot

private final class MoveMonitor(
    system: ActorSystem,
    channel: ActorRef) {

  Kamon.metrics.subscribe("trace", "**", system.actorOf(Props(new Actor {
    def receive = {
      case tick: TickMetricSnapshot => tick.metrics foreach {
        case (entity, snapshot) =>
          println("-------- " + entity)
          println(snapshot)
          println(snapshot.histogram("histogram"))
          println(snapshot.histogram("histogram").foreach { h =>
            println(h.sum)
            println(h.numberOfMeasurements)
          })
      }
    }
  })))

  // Kamon.metrics.subscribe("histogram", "round.move.trace.elapsed-time", system.actorOf(Props(new Actor {
  //   def receive = {
  //     case tick: TickMetricSnapshot => tick.pp.metrics.pp.collectFirst {
  //       case (entity, snapshot) if entity.category == "histogram" => snapshot
  //     } flatMap (_ histogram "histogram") foreach { h =>
  //       if (!h.pp.isEmpty.pp) channel ! lila.socket.Channel.Publish(
  //         lila.socket.Socket.makeMessage("mlat", (h.sum / h.numberOfMeasurements / 1000000).toInt)
  //       )
  //     }
  //   }
  // })))
}
