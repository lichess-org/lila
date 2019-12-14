package lila.memo

import scala.concurrent.duration.FiniteDuration
import play.api.mvc.Result
import play.api.mvc.Results.TooManyRequests
import play.api.libs.json.Json

/**
 * only allow one future at a time per key
 */
final class LinearLimit[K](
    name: String,
    key: String,
    ttl: FiniteDuration,
    limitedDefault: Fu[Result] = fuccess(TooManyRequests(Json.obj("error" -> "Try again later"))),
    toString: K => String = (k: K) => k.toString
)(implicit ec: scala.concurrent.ExecutionContext) {
  private val storage = new ExpireSetMemo(ttl)

  private lazy val logger = lila.log("linearlimit").branch(name)
  private lazy val monitor = lila.mon.security.linearLimit(key)

  def apply(k: K, msg: => String = "", limited: => Fu[Result] = limitedDefault)(f: => Fu[Result]): Fu[Result] =
    if (storage get toString(k)) {
      logger.info(s"$k $msg")
      monitor.increment()
      limited
    } else {
      storage put toString(k)
      f addEffectAnyway {
        storage remove toString(k)
      }
    }
}
