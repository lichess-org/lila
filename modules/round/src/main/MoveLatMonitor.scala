package lila.round

import java.util.concurrent.atomic.AtomicReference

private object MoveLatMonitor:

  private case class Latency(totalMillis: Long = 0, count: Int = 0):
    def recordMillis(millis: Int) = copy(totalMillis + millis, count + 1)
    def average = (totalMillis / count.atLeast(1)).toInt
  private val latency = AtomicReference(Latency())

  private def microsToMillis(micros: Int) = Math.ceil(micros.toFloat / 1000).toInt

  def recordMicros(micros: Int): Unit =
    latency.getAndUpdate(_.recordMillis(microsToMillis(micros)))

  object wsLatency:
    var latestMillis = 0
    def set(millis: Int) = latestMillis = millis

  def start(scheduler: Scheduler)(using Executor) =
    scheduler.scheduleWithFixedDelay(10.second, 2.second): () =>
      val full = latency.getAndSet(Latency()).average + wsLatency.latestMillis
      lila.common.Bus.pub(lila.core.round.Mlat(full))
