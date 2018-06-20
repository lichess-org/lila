package lila.round

import akka.actor._
import com.typesafe.config.Config
import kamon._
import kamon.MetricReporter
import scala.concurrent.duration._

private object MoveMonitor {

  def start(system: ActorSystem, channel: ActorRef) = {
    var current: Int = 0
    system.scheduler.schedule(5 second, 2 second) {
      channel ! lila.socket.Channel.Publish(lila.socket.Socket.makeMessage("mlat", current))
    }
    Kamon.addReporter(new MetricReporter {
      def start() = {}
      def stop() = {}
      def reconfigure(config: Config) = {}
      def reportPeriodSnapshot(snapshot: kamon.metric.PeriodSnapshot): Unit =
        ???
      // snapshot.metrics.histograms.collectFirst {
      //   case distribution if distribution.name == "trace" => snapshot
      // } flatMap (_ histogram "elapsed-time") foreach { h =>
      //   if (!h.isEmpty) current = (h.sum / h.numberOfMeasurements / 1000000).toInt pp "current!"
      // }
    }, "trace", "round.move.trace")
  }
}
