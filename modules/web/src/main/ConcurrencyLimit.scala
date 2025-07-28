package lila.web

import akka.stream.scaladsl.*
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.TooManyRequests

/** only allow X streams at a time per key */
final class ConcurrencyLimit[K](
    name: String,
    key: String,
    ttl: FiniteDuration,
    maxConcurrency: Int = 1,
    limitedDefault: Int => Result = ConcurrencyLimit.limitedDefault,
    toString: K => String = (k: K) => k.toString
)(using Executor):

  private val storage = ConcurrencyLimit.Storage(ttl, maxConcurrency, toString)
  private val logger = lila.log("concurrencylimit").branch(name)
  private val monitor = lila.mon.security.concurrencyLimit(key)

  def compose[T](k: K, msg: => String = ""): Option[Source[T, ?] => Source[T, ?]] =
    if storage.get(k) >= maxConcurrency then
      logger.info(s"$k $msg")
      monitor.increment()
      none
    else
      storage.inc(k)
      some:
        _.watchTermination(): (_, done) =>
          done.onComplete: _ =>
            storage.dec(k)

  def apply[T](k: K, msg: => String = "")(
      makeSource: => Source[T, ?]
  )(makeResult: Source[T, ?] => Result): Result =
    compose[T](k, msg).fold(limitedDefault(maxConcurrency)): watch =>
      makeResult(watch(makeSource))

object ConcurrencyLimit:

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
