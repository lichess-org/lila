package lila.memo

import play.api.mvc.Result
import scala.concurrent.duration.FiniteDuration

/**
  * only allow one future at a time per key
  */
final class FutureConcurrencyLimit[K](
    key: String,
    ttl: FiniteDuration,
    maxConcurrency: Int = 1,
    toString: K => String = (k: K) => k.toString
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val storage = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[String, Int]()

  private val concurrentMap = storage.underlying.asMap

  private lazy val monitor = lila.mon.security.concurrencyLimit(key)

  def apply(k: K, limited: => Fu[Result])(op: => Fu[Result]): Fu[Result] =
    get(k) match {
      case c @ _ if c >= maxConcurrency =>
        monitor.increment()
        limited
      case c @ _ =>
        inc(k)
        op addEffectAnyway {
          dec(k)
        }
    }

  private def get(k: K) = ~storage.getIfPresent(toString(k))
  private def inc(k: K) = concurrentMap.compute(toString(k), (_, c) => (~Option(c) + 1) atMost maxConcurrency)
  private def dec(k: K) = concurrentMap.computeIfPresent(toString(k), (_, c) => (c - 1) atLeast 0)
}
