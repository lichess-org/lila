package lila.memo

import alleycats.Zero

import lila.core.config.RateLimit as Enforce

/** Throttler that allows X operations per Y unit of time Not thread safe
  */
final class RateLimit[K](
    credits: Int,
    duration: FiniteDuration,
    key: String,
    log: Boolean = true
)(using enforce: Enforce)(using Executor)
    extends RateLimit.RateLimiter[K]:
  import RateLimit.*

  private val storage = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(duration)
    .build[K, (Cost, ClearAt)]()

  private inline def makeClearAt = nowMillis + duration.toMillis

  private lazy val logger  = lila.log("ratelimit").branch(key)
  private lazy val monitor = lila.mon.security.rateLimit(key)

  def chargeable[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(
      op: ChargeWith => A
  ): A =
    apply(k, default, cost, msg):
      op(c => apply(k, {}, c, s"charge: $msg") {})

  def apply[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(op: => A): A =
    if cost < 1 then op
    else
      storage.getIfPresent(k) match
        case None =>
          storage.put(k, cost -> makeClearAt)
          op
        case Some(a, clearAt) if a < credits =>
          storage.put(k, (a + cost) -> clearAt)
          op
        case Some(_, clearAt) if nowMillis > clearAt =>
          storage.put(k, cost -> makeClearAt)
          op
        case _ if enforce.yes =>
          if log then logger.info(s"$credits/$duration $k cost: $cost $msg")
          monitor.increment()
          default
        case _ =>
          op

  def zero[A](k: K, cost: Cost = 1, msg: => String = "")(op: => A)(using default: Zero[A]): A =
    apply[A](k, default.zero, cost, msg)(op)

object RateLimit:

  type ChargeWith = Cost => Unit
  type Charge     = () => Unit
  type Cost       = Int

  enum Result:
    case Through, Limited

  trait RateLimiter[K]:
    def apply[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(op: => A): A
    def chargeable[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(op: ChargeWith => A): A

  def composite[K](
      key: String,
      log: Boolean = true
  )(rules: (String, Int, FiniteDuration)*)(using Executor, Enforce): RateLimiter[K] =

    val limiters: Seq[RateLimit[K]] = rules.map: (subKey, credits, duration) =>
      RateLimit[K](
        credits = credits,
        duration = duration,
        key = s"$key.$subKey",
        log = log
      )

    new RateLimiter[K]:

      def apply[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(op: => A): A =
        val accepted = limiters.foldLeft(true):
          case (true, limiter) => limiter(k, false, cost, msg)(true)
          case (false, _)      => false
        if accepted then op else default

      def chargeable[A](k: K, default: => A, cost: Cost = 1, msg: => String = "")(op: ChargeWith => A): A =
        apply(k, default, cost, msg):
          op(c => apply(k, default, c, s"charge: $msg") {})

  private type ClearAt = Long
