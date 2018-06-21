package lila.round

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

private object MoveMonitor {

  private val metricName = "round.move.segments.full"

  def start(system: ActorSystem, channel: ActorRef) = {
    var current: Int = 0
    system.scheduler.schedule(5 second, 2 second) {
      channel ! lila.socket.Channel.Publish(lila.socket.Socket.makeMessage("mlat", current))
    }
    kamon.Kamon.addReporter(new kamon.MetricReporter {
      def start() = {}
      def stop() = {}
      def reconfigure(config: Config) = {}
      def reportPeriodSnapshot(snapshot: kamon.metric.PeriodSnapshot): Unit = {
        snapshot.metrics.histograms.collectFirst {
          case hist if hist.name == metricName && hist.distribution.count > 0 =>
            current = (hist.distribution.percentile(50).value / 1000000).toInt
        }
      }
    }, "move latency reporter")
  }
}
