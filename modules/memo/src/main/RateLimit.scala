package lila.memo

import scala.concurrent.duration.FiniteDuration

/** Throttler that allows X operations per Y unit of time
  * Not thread safe
  */
final class RateLimit[K](
    credits: Int,
    duration: FiniteDuration,
    key: String,
    enforce: Boolean = true,
    log: Boolean = true
) extends RateLimit.RateLimiter[K] {
  import RateLimit._

  private val storage = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(duration)
    .build[K, (Cost, ClearAt)]()

  private def makeClearAt = nowMillis + duration.toMillis

  private lazy val logger  = lila.log("ratelimit").branch(key)
  private lazy val monitor = lila.mon.security.rateLimit(key)

  def chargeable[A](k: K, cost: Cost = 1, msg: => String = "")(
      op: Charge => A
  )(default: => A): A =
    apply(k, cost, msg) { op(c => apply(k, c, s"charge: $msg") {} {}) }(default)

  def apply[A](k: K, cost: Cost = 1, msg: => String = "")(op: => A)(default: => A): A =
    if (cost < 1) op
    else
      storage getIfPresent k match {
        case None =>
          storage.put(k, cost -> makeClearAt)
          op
        case Some((a, clearAt)) if a < credits =>
          storage.put(k, (a + cost) -> clearAt)
          op
        case Some((_, clearAt)) if nowMillis > clearAt =>
          storage.put(k, cost -> makeClearAt)
          op
        case _ if enforce =>
          if (log) logger.info(s"$credits/$duration $k cost: $cost $msg")
          monitor.increment()
          default
        case _ =>
          op
      }
}

object RateLimit {

  type Charge = Cost => Unit
  type Cost   = Int

  trait RateLimiter[K] {

    def apply[A](k: K, cost: Cost = 1, msg: => String = "")(op: => A)(default: => A): A

    def chargeable[A](k: K, cost: Cost = 1, msg: => String = "")(op: Charge => A)(default: => A): A
  }

  sealed trait Result
  case object Through extends Result
  case object Limited extends Result

  def composite[K](
      key: String,
      enforce: Boolean = true,
      log: Boolean = true
  )(rules: (String, Int, FiniteDuration)*): RateLimiter[K] = {

    val limiters: Seq[RateLimit[K]] = rules.map { case (subKey, credits, duration) =>
      new RateLimit[K](
        credits = credits,
        duration = duration,
        key = s"$key.$subKey",
        enforce = enforce,
        log = log
      )
    }

    new RateLimiter[K] {

      def apply[A](k: K, cost: Cost = 1, msg: => String = "")(op: => A)(default: => A): A = {
        val accepted = limiters.foldLeft(true) {
          case (true, limiter) => limiter(k, cost, msg)(true)(false)
          case (false, _)      => false
        }
        if (accepted) op else default
      }

      def chargeable[A](k: K, cost: Cost = 1, msg: => String = "")(op: Charge => A)(default: => A): A = {
        apply(k, cost, msg) { op(c => apply(k, c, s"charge: $msg") {} {}) }(default)
      }
    }
  }

  private type ClearAt = Long
}
