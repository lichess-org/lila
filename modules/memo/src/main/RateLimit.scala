package lila.memo

import scala.concurrent.duration.Duration
import ornicar.scalalib.Zero

/**
 * very simple side effect throttler
 * that allows one call per duration,
 * and discards the rest
 */
final class RateLimitGlobal(duration: Duration) {

  private val durationMillis = duration.toMillis

  private var lastHit: Long = nowMillis - durationMillis - 10

  def apply[A: Zero](op: => A): A = {
    (nowMillis > lastHit + durationMillis) ?? {
      lastHit = nowMillis
      op
    }
  }
}

/**
 * very simple side effect throttler
 * that allows one call per duration and per key,
 * and discards the rest
 */
final class RateLimitByKey(duration: Duration) {

  private val storage = new ExpireSetMemo(ttl = duration)

  def apply[A: Zero](key: String)(op: => A): A = {
    (!storage.get(key)) ?? {
      storage put key
      op
    }
  }
}
