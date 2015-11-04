package lila.security

import scala.concurrent.duration.Duration
import lila.memo.ExpireSetMemo

/** very simple side effect throttler
 that allows one call per duration,
 and discards the rest */
final class RateLimitGlobal(duration: Duration) {

  private val durationMillis = duration.toMillis

  private var lastHit: Long = nowMillis - durationMillis - 10

  def apply(op: => Unit) {
    if (nowMillis > lastHit + durationMillis) {
      lastHit = nowMillis
      op
    }
  }
}
