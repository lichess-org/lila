package lila.memo

import akka.stream.scaladsl._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.TooManyRequests
import scala.concurrent.duration.FiniteDuration

/**
  * only allow one stream at a time per key
  */
final class ConcurrencyLimit[K](
    name: String,
    key: String,
    ttl: FiniteDuration,
    maxConcurrency: Int = 1,
    limitedDefault: Int => Result = ConcurrencyLimit.limitedDefault _,
    toString: K => String = (k: K) => k.toString
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val storage = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[String, Int]()

  private val concurrentMap = storage.underlying.asMap

  private lazy val logger  = lila.log("concurrencylimit").branch(name)
  private lazy val monitor = lila.mon.security.concurrencyLimit(key)

  def compose[T](k: K, msg: => String = ""): Option[Source[T, _] => Source[T, _]] =
    get(k) match {
      case c @ _ if c >= maxConcurrency =>
        logger.info(s"$k $msg")
        monitor.increment()
        none
      case c @ _ =>
        inc(k)
        some {
          _.watchTermination() { (_, done) =>
            done.onComplete { _ =>
              dec(k)
            }
          }
        }
    }

  def apply[T](k: K, msg: => String = "")(
      makeSource: => Source[T, _]
  )(makeResult: Source[T, _] => Result): Result =
    compose[T](k, msg).fold(limitedDefault(maxConcurrency)) { watch =>
      makeResult(watch(makeSource))
    }

  private def get(k: K) = ~storage.getIfPresent(toString(k))
  private def inc(k: K) = concurrentMap.compute(toString(k), (_, c) => (~Option(c) + 1) atMost maxConcurrency)
  private def dec(k: K) = concurrentMap.computeIfPresent(toString(k), (_, c) => (c - 1) atLeast 0)
}

object ConcurrencyLimit {

  def limitedDefault(max: Int) =
    TooManyRequests(Json.obj("error" -> s"Please only run $max request(s) at a time"))
}
