package lila.memo

import ornicar.scalalib.Zero
import scala.concurrent.duration.Duration

/**
 * side effect throttler that allows X ops per Y unit of time
 */
final class RateLimit(nb: Int, duration: Duration, name: String) {

  private type NbOps = Int
  private type ClearAt = Long

  private val storage = Builder.expiry[String, (NbOps, ClearAt)](ttl = duration)

  private def makeClearAt = nowMillis + duration.toMillis

  private val logger = play.api.Logger("ratelimit")

  def apply[A](key: String)(op: => A)(implicit default: Zero[A]): A =
    Option(storage getIfPresent key) match {
      case None =>
        storage.put(key, 1 -> makeClearAt)
        op
      case Some((a, clearAt)) if a <= nb =>
        storage.put(key, (a + 1) -> clearAt)
        op
      case Some((_, clearAt)) if nowMillis > clearAt =>
        storage.put(key, 1 -> makeClearAt)
        op
      case _ =>
        logger.info(s"$name ($nb/$duration) $key")
        default.zero
    }
}
