package lila.round

import akka.actor.Scheduler
import scala.concurrent.duration._
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }

private object MoveLatMonitor {

  private var totalMicros = new AtomicLong
  private var count = new AtomicInteger

  def start(scheduler: Scheduler) =
    scheduler.scheduleWithFixedDelay(10 second, 2 second) { () =>
      val average = (totalMicros.getAndSet(0) / count.getAndSet(0).atLeast(1)).toInt
      lila.common.Bus.publish(lila.hub.actorApi.round.Mlat(average), "mlat")
    }

  def record(micros: Int) = {
    totalMicros getAndAdd micros
    count.getAndIncrement
  }
}
