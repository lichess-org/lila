package lila.round

import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot

private final class MoveMonitor(
    system: ActorSystem,
    channel: ActorRef) {

  private val timeName = "round.move.time"
  private val countName = "round.move.count"

  private val time = Kamon.metrics.histogram(timeName)
  private val count = Kamon.metrics.counter(countName)

  def record(ms: Option[Int]) = {
    ms foreach { x => time.record(x) }
    count.increment()
  }

  Kamon.metrics.subscribe("histogram", timeName, system.actorOf(Props(new Actor {
    def receive = {
      case tick: TickMetricSnapshot => tick.metrics.collectFirst {
        case (entity, snapshot) if entity.category == "histogram" => snapshot
      } flatMap (_ histogram "histogram") foreach { h =>
        if (!h.isEmpty) channel ! lila.socket.Channel.Publish(
          lila.socket.Socket.makeMessage("mlat", (h.sum / h.numberOfMeasurements).toInt)
        )
      }
    }
  })))
}
