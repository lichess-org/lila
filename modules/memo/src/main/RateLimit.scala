package lila.memo

import ornicar.scalalib.Zero
import scala.concurrent.duration.Duration

/**
 * side effect throttler that allows X ops per Y unit of time
 */
final class RateLimit(nb: Int, duration: Duration) {

  private val storage = Builder.expiry[String, Int](ttl = duration)

  def apply[A](key: String)(op: => A)(implicit default: Zero[A]): A =
    ~Option(storage getIfPresent key) match {
      case a if a <= nb =>
        storage.put(key, a + 1); op
      case _ => default.zero
    }
}
