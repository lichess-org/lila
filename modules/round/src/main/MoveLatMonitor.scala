package lila.round

import akka.actor.Scheduler
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration._

private object MoveLatMonitor {

  private case class Latency(totalMillis: Long = 0, count: Int = 0) {
    def recordMillis(millis: Int) = copy(totalMillis + millis, count + 1)
    def average                   = (totalMillis / count.atLeast(1)).toInt
  }
  private val latency = new AtomicReference(Latency())

  private def microsToMillis(micros: Int) = Math.ceil(micros.toFloat / 1000).toInt

  def recordMicros(micros: Int): Unit =
    latency.getAndUpdate(_ recordMillis microsToMillis(micros)).unit

  object wsLatency {
    var latestMillis     = 0
    def set(millis: Int) = latestMillis = millis
  }

  def start(scheduler: Scheduler)(implicit ec: scala.concurrent.ExecutionContext) =
    scheduler.scheduleWithFixedDelay(10 second, 2 second) { () =>
      val full = latency.getAndSet(Latency()).average + wsLatency.latestMillis
      lila.common.Bus.publish(lila.hub.actorApi.round.Mlat(full), "mlat")
    }
}
