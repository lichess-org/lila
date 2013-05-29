package lila.hub

import scala.concurrent.duration.Duration

final class TimeBomb(delayDuration: Duration) {

  private var explodesAt: Double = _

  delay

  def delay { 
    explodesAt = nowMillis + delayDuration.toMillis
  }

  def boom = explodesAt < nowMillis
}
