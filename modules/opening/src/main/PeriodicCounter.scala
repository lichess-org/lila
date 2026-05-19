package lila.opening

/* Counts hits over a period of time, using only 2 integers. */
final private class PeriodicCounter(period: FiniteDuration)(using scheduler: Scheduler)(using Executor):

  private var prevPeriodCount = 0
  private var currentPeriodCount = 0

  def increment(): Unit = currentPeriodCount += 1

  def get: Int = Math.max(prevPeriodCount, currentPeriodCount)

  scheduler.scheduleAtFixedRate(period, period): () =>
    prevPeriodCount = currentPeriodCount
    currentPeriodCount = 0
