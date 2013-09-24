package lila.hub

import scala.concurrent.duration.Duration

final class TimeBomb(delayDuration: Duration) {

  private val delayMs = delayDuration.toMillis

  private var delayedAt: Double = nowMillis

  def delay {
    delayedAt = nowMillis
  }

  def boom = ((delayedAt + delayMs) < nowMillis)
}
