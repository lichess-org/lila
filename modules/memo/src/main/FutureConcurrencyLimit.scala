package lila.memo

import play.api.mvc.Result
import scala.concurrent.duration.FiniteDuration

/** only allow X futures at a time per key
  */
final class FutureConcurrencyLimit[K](
    key: String,
    ttl: FiniteDuration,
    maxConcurrency: Int = 1,
    toString: K => String = (k: K) => k.toString
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val storage = new ConcurrencyLimit.Storage(ttl, maxConcurrency, toString)

  private lazy val monitor = lila.mon.security.concurrencyLimit(key)

  def apply(k: K, limited: => Fu[Result])(op: => Fu[Result]): Fu[Result] =
    storage.get(k) match {
      case c @ _ if c >= maxConcurrency =>
        monitor.increment()
        limited
      case c @ _ =>
        storage.inc(k)
        op addEffectAnyway {
          storage.dec(k).unit
        }
    }
}
