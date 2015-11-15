package lila.memo

import ornicar.scalalib.Zero
import scala.concurrent.duration.Duration

/**
 * very simple side effect throttler
 * that allows one call per duration,
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
 * side effect throttler that allows X ops per Y unit of time
 */
final class RateLimitByKey(nb: Int, duration: Duration) {

  private val storage = Builder.expiry[String, Int](ttl = duration)

  def apply[A](key: String)(op: => A)(implicit default: Zero[A]): A =
    ~Option(storage getIfPresent key) match {
      case a if a <= nb =>
        storage.put(key, a + 1); op
      case _ => default.zero
    }
}
