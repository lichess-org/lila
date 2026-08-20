package lila.web

import org.apache.pekko.stream.scaladsl.*
import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }
import play.api.mvc.Results.TooManyRequests
import lila.common.HTTPRequest

/** only allow X streams at a time per key */
final class ConcurrencyLimit[K](
    maxConcurrency: Int,
    key: String,
    ttl: FiniteDuration = 1.hour,
    limitedDefault: Int => Result = ConcurrencyLimit.limitedDefault,
    toString: K => String = (k: K) => k.toString
)(using Executor):

  import ConcurrencyLimit.{ Storage, Limiter }

  private val storage = ConcurrencyLimit.Storage(ttl, maxConcurrency, toString)
  private val limitedMon = lila.mon.security.concurrencyLimit(key)
  private def levelMon(k: K) = lila.mon.security.concurrencyLevel(key, toString(k))

  def compose[T](k: K)(using RequestHeader): Option[Source[T, ?] => Source[T, ?]] =
    if storage.get(k) >= maxConcurrency then
      lila.memo.RateLimit.logger.info(s"concurrency $key $k $reqMsg")
      limitedMon.increment()
      none
    else
      val level = storage.inc(k)
      if level >= 3 then levelMon(k).update(level)
      some:
        _.watchTermination(): (_, done) =>
          done.onComplete: _ =>
            val level = storage.dec(k)
            if level >= 2 then levelMon(k).update(level)

  def apply[T](k: K)(using RequestHeader): Limiter[T] = makeSource =>
    makeResult =>
      compose[T](k).fold(limitedDefault(maxConcurrency)): watch =>
        makeResult(watch(makeSource))

  private def reqMsg(using req: RequestHeader) = s"${req.path} ${HTTPRequest.userAgent(req)}"

object ConcurrencyLimit:

  type Limiter[T] = (=> Source[T, ?]) => (Source[T, ?] => Result) => Result

  final class Storage[K](
      ttl: FiniteDuration,
      maxConcurrency: Int,
      toString: K => String = (k: K) => k.toString
  )(using Executor):
    private val storage = lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterWrite(ttl)
      .build[String, Int]()

    private val concurrentMap = storage.underlying.asMap

    def get(k: K) = storage.getIfPresent(toString(k)).orZero
    def inc(k: K) = concurrentMap.compute(toString(k), (_, c) => (~Option(c) + 1).atMost(maxConcurrency))
    def dec(k: K) = concurrentMap.computeIfPresent(toString(k), (_, c) => (c - 1).atLeast(0))

  def limitedDefault(max: Int) =
    TooManyRequests(Json.obj("error" -> s"Please only run $max request(s) at a time"))

/** only allow X futures at a time per key */
final class FutureConcurrencyLimit[K](
    key: String,
    ttl: FiniteDuration,
    maxConcurrency: Int = 1,
    toString: K => String = (k: K) => k.toString
)(using Executor):

  private val storage = ConcurrencyLimit.Storage(ttl, maxConcurrency, toString)

  private lazy val monitor = lila.mon.security.concurrencyLimit(key)

  def apply[A](k: K, limited: => Fu[A])(op: => Fu[A]): Fu[A] =
    if storage.get(k) >= maxConcurrency then
      monitor.increment()
      limited
    else
      storage.inc(k)
      op.addEffectAnyway:
        storage.dec(k)
